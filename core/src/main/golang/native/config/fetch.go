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
	ctx, cancel := context.WithTimeout(context.Background(), 60*time.Second)
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
// It checks for common top-level keys that indicate Clash format.
func isClashYAML(data []byte) bool {
	// Quick check: YAML configs typically start with a key or comment
	trimmed := bytes.TrimSpace(data)
	if len(trimmed) == 0 {
		return false
	}

	// If it starts with '{' it's likely JSON (which YAML can parse), allow it
	if trimmed[0] == '{' {
		return true
	}

	// Check for common Clash YAML top-level keys
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

// generateProviderConfig creates a standard Clash YAML config that uses
// the original subscription URL as a proxy-provider. This follows the
// upstream mihomo pattern where format conversion (Base64/URI → proxies)
// is handled by the provider parser (provider.go:371).
func generateProviderConfig(subscriptionURL string, providerPath string) []byte {
	cfg := fmt.Sprintf(`# Auto-generated config for non-YAML subscription
# Original URL: %s

mixed-port: 7890
mode: rule
log-level: info

proxy-providers:
  subscription:
    type: http
    url: "%s"
    path: "%s"
    interval: 3600
    health-check:
      enable: true
      url: "https://www.gstatic.com/generate_204"
      interval: 300

proxy-groups:
  - name: PROXY
    type: select
    use:
      - subscription
    proxies:
      - auto

  - name: auto
    type: url-test
    use:
      - subscription
    url: "https://www.gstatic.com/generate_204"
    interval: 300

rules:
  - MATCH,PROXY
`, subscriptionURL, subscriptionURL, providerPath)

	return []byte(cfg)
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

		// Check if the downloaded content is valid Clash YAML.
		// If not (e.g. Base64-encoded proxy URIs), generate a config
		// that uses the URL as a proxy-provider, letting the upstream
		// provider parser handle the format conversion.
		configData, readErr := os.ReadFile(configPath)
		if readErr == nil && !isClashYAML(configData) {
			// Verify the content is actually convertible
			proxies, convErr := convert.ConvertsV2Ray(configData)
			if convErr == nil && len(proxies) > 0 {
				log.Infoln("Subscription returned %d proxies in URI format, generating provider-based config", len(proxies))

				providerDir := P.Join(path, "providers")
				_ = os.MkdirAll(providerDir, 0700)
				providerPath := P.Join(providerDir, "subscription.yaml")

				// Move the original content to the provider file
				_ = os.Rename(configPath, providerPath)

				// Write a proper Clash config referencing the subscription as provider
				generatedConfig := generateProviderConfig(url, providerPath)
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
