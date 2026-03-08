package config

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	U "net/url"
	"os"
	P "path"
	"runtime"
	"strings"
	"time"

	"cfa/native/app"

	"github.com/metacubex/mihomo/common/convert"
	clashHttp "github.com/metacubex/mihomo/component/http"
	"github.com/metacubex/mihomo/log"
)

type Status struct {
	Action      string   `json:"action"`
	Args        []string `json:"args"`
	Progress    int      `json:"progress"`
	MaxProgress int      `json:"max"`
}

func openUrl(ctx context.Context, url string) (io.ReadCloser, error) {
	response, err := clashHttp.HttpRequest(ctx, url, http.MethodGet, http.Header{"User-Agent": {"YueTong/" + app.VersionName()}}, nil)

	if err != nil {
		return nil, err
	}

	return response.Body, nil
}

func openContent(url string) (io.ReadCloser, error) {
	return app.OpenContent(url)
}

func fetch(url *U.URL, file string) error {
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Minute)
	defer cancel()

	var reader io.ReadCloser
	var err error

	switch url.Scheme {
	case "http", "https":
		reader, err = openUrl(ctx, url.String())
	case "content":
		reader, err = openContent(url.String())
	default:
		err = fmt.Errorf("unsupported scheme %s of %s", url.Scheme, url)
	}

	if err != nil {
		return err
	}

	defer reader.Close()

	_ = os.MkdirAll(P.Dir(file), 0700)

	f, err := os.OpenFile(file, os.O_WRONLY|os.O_TRUNC|os.O_CREATE, 0600)
	if err != nil {
		return err
	}

	defer f.Close()

	_, err = io.Copy(f, reader)
	if err != nil {
		_ = os.Remove(file)
	}

	return err
}

// isClashYAML checks if the content looks like a valid Clash YAML config.
func isClashYAML(data []byte) bool {
	trimmed := bytes.TrimSpace(data)
	if len(trimmed) == 0 {
		return false
	}

	// JSON is valid YAML
	if trimmed[0] == '{' {
		return true
	}

	clashKeys := []string{
		"proxies:", "proxy-groups:", "proxy-providers:",
		"rules:", "mixed-port:", "port:", "socks-port:",
		"dns:", "tun:", "listeners:", "mode:",
	}
	for _, key := range clashKeys {
		if bytes.Contains(trimmed, []byte(key)) {
			return true
		}
	}

	return false
}

// convertToClashConfig converts Base64/URI proxy list to a full Clash YAML config
// with proxies inline. No proxy-provider indirection, no runtime re-download.
func convertToClashConfig(proxies []map[string]any) []byte {
	var b strings.Builder

	b.WriteString("mixed-port: 7890\n")
	b.WriteString("mode: rule\n")
	b.WriteString("log-level: info\n\n")

	// proxies section
	b.WriteString("proxies:\n")
	var names []string
	for _, proxy := range proxies {
		name, _ := proxy["name"].(string)
		if name == "" {
			continue
		}
		names = append(names, name)

		b.WriteString("  - {")
		first := true
		// name and type first
		for _, key := range []string{"name", "type"} {
			if val, ok := proxy[key]; ok {
				if !first {
					b.WriteString(", ")
				}
				b.WriteString(fmt.Sprintf("%s: %s", key, yamlValue(val)))
				first = false
			}
		}
		// rest of fields
		for key, val := range proxy {
			if key == "name" || key == "type" {
				continue
			}
			b.WriteString(fmt.Sprintf(", %s: %s", key, yamlValue(val)))
		}
		b.WriteString("}\n")
	}

	// proxy-groups
	b.WriteString("\nproxy-groups:\n")
	b.WriteString("  - name: PROXY\n")
	b.WriteString("    type: select\n")
	b.WriteString("    proxies:\n")
	b.WriteString("      - auto\n")
	for _, name := range names {
		b.WriteString(fmt.Sprintf("      - \"%s\"\n", strings.ReplaceAll(name, "\"", "\\\"")))
	}

	b.WriteString("\n  - name: auto\n")
	b.WriteString("    type: url-test\n")
	b.WriteString("    url: \"https://www.gstatic.com/generate_204\"\n")
	b.WriteString("    interval: 300\n")
	b.WriteString("    proxies:\n")
	for _, name := range names {
		b.WriteString(fmt.Sprintf("      - \"%s\"\n", strings.ReplaceAll(name, "\"", "\\\"")))
	}

	// rules
	b.WriteString("\nrules:\n")
	b.WriteString("  - MATCH,PROXY\n")

	return []byte(b.String())
}

// yamlValue formats a Go value for inline YAML output.
func yamlValue(v any) string {
	switch val := v.(type) {
	case string:
		// Quote strings that contain special chars
		if strings.ContainsAny(val, ":{}[],&*#?|-<>=!%@`\"'\n") || val == "" || val == "true" || val == "false" {
			return fmt.Sprintf("\"%s\"", strings.ReplaceAll(val, "\"", "\\\""))
		}
		return val
	case bool:
		if val {
			return "true"
		}
		return "false"
	case []string:
		items := make([]string, len(val))
		for i, s := range val {
			items[i] = fmt.Sprintf("\"%s\"", s)
		}
		return "[" + strings.Join(items, ", ") + "]"
	case map[string]any:
		// Nested map — serialize as JSON-like inline
		data, err := json.Marshal(val)
		if err != nil {
			return "{}"
		}
		return string(data)
	default:
		return fmt.Sprintf("%v", val)
	}
}

func FetchAndValid(
	path string,
	url string,
	force bool,
	reportStatus func(string),
) error {
	configPath := P.Join(path, "config.yaml")

	if _, err := os.Stat(configPath); os.IsNotExist(err) || force {
		parsedURL, err := U.Parse(url)
		if err != nil {
			return err
		}

		bytes, _ := json.Marshal(&Status{
			Action:      "FetchConfiguration",
			Args:        []string{parsedURL.Host},
			Progress:    -1,
			MaxProgress: -1,
		})

		reportStatus(string(bytes))

		if err := fetch(parsedURL, configPath); err != nil {
			return err
		}

		// If downloaded content is not Clash YAML (e.g. Base64 proxy URIs),
		// convert it directly to a Clash config with proxies inline.
		configData, readErr := os.ReadFile(configPath)
		if readErr == nil && !isClashYAML(configData) {
			proxies, convErr := convert.ConvertsV2Ray(configData)
			if convErr == nil && len(proxies) > 0 {
				log.Infoln("Subscription returned %d proxies in URI format, converting to inline config", len(proxies))
				generatedConfig := convertToClashConfig(proxies)
				_ = os.WriteFile(configPath, generatedConfig, 0600)
			}
		}
	}

	defer runtime.GC()

	rawCfg, err := UnmarshalAndPatch(path)
	if err != nil {
		return err
	}

	forEachProviders(rawCfg, func(index int, total int, name string, provider map[string]any, prefix string) {
		bytes, _ := json.Marshal(&Status{
			Action:      "FetchProviders",
			Args:        []string{name},
			Progress:    index,
			MaxProgress: total,
		})

		reportStatus(string(bytes))

		u, uok := provider["url"]
		p, pok := provider["path"]

		if !uok || !pok {
			return
		}

		us, uok := u.(string)
		ps, pok := p.(string)

		if !uok || !pok {
			return
		}

		if _, err := os.Stat(ps); err == nil {
			return
		}

		url, err := U.Parse(us)
		if err != nil {
			return
		}

		_ = fetch(url, ps)
	})

	bytes, _ := json.Marshal(&Status{
		Action:      "Verifying",
		Args:        []string{},
		Progress:    0xffff,
		MaxProgress: 0xffff,
	})

	reportStatus(string(bytes))

	cfg, err := Parse(rawCfg)
	if err != nil {
		return err
	}

	destroyProviders(cfg)

	return nil
}
