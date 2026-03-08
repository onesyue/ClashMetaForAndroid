package config

import (
	"fmt"
	"os"
	P "path"
	"runtime"
	"strings"

	"cfa/native/app"

	"github.com/metacubex/mihomo/common/convert"
	"github.com/metacubex/mihomo/common/yaml"
	"github.com/metacubex/mihomo/config"
	"github.com/metacubex/mihomo/hub"
	"github.com/metacubex/mihomo/log"
)

func logDns(cfg *config.RawConfig) {
	bytes, err := yaml.Marshal(&cfg.DNS)
	if err != nil {
		log.Warnln("Marshal dns: %s", err.Error())

		return
	}

	log.Infoln("dns:")

	for _, line := range strings.Split(string(bytes), "\n") {
		log.Infoln("  %s", line)
	}
}

func UnmarshalAndPatch(profilePath string) (*config.RawConfig, error) {
	configPath := P.Join(profilePath, "config.yaml")

	configData, err := os.ReadFile(configPath)
	if err != nil {
		return nil, err
	}

	rawConfig, err := config.UnmarshalRawConfig(configData)
	if err != nil {
		// YAML parsing failed — try converting as Base64/URI subscription
		// (e.g. hysteria2://, vless://, ss:// links, possibly Base64-encoded)
		log.Infoln("YAML unmarshal failed, trying V2Ray subscription conversion")

		proxies, convErr := convert.ConvertsV2Ray(configData)
		if convErr != nil || len(proxies) == 0 {
			// Conversion also failed — return original YAML error
			return nil, fmt.Errorf("not a valid Clash config or subscription: %w", err)
		}

		log.Infoln("Converted %d proxies from subscription format", len(proxies))

		// Build a minimal Clash config with the converted proxies and a
		// default proxy group so the config is usable out of the box.
		proxyNames := make([]any, 0, len(proxies))
		for _, p := range proxies {
			if name, ok := p["name"].(string); ok {
				proxyNames = append(proxyNames, name)
			}
		}

		rawConfig = config.DefaultRawConfig()
		rawConfig.Proxy = proxies
		rawConfig.ProxyGroup = []map[string]any{
			{
				"name":     "PROXY",
				"type":     "select",
				"proxies":  proxyNames,
			},
			{
				"name":     "auto",
				"type":     "url-test",
				"proxies":  proxyNames,
				"url":      "https://www.gstatic.com/generate_204",
				"interval": 300,
			},
		}

		// Write the converted YAML back so subsequent loads don't need conversion
		if yamlData, marshalErr := yaml.Marshal(rawConfig); marshalErr == nil {
			_ = os.WriteFile(configPath, yamlData, 0600)
		}
	}

	if err := process(rawConfig, profilePath); err != nil {
		return nil, err
	}

	return rawConfig, nil
}

func Parse(rawConfig *config.RawConfig) (*config.Config, error) {
	cfg, err := config.ParseRawConfig(rawConfig)
	if err != nil {
		return nil, err
	}

	return cfg, nil
}

func Load(path string) error {
	rawCfg, err := UnmarshalAndPatch(path)
	if err != nil {
		log.Errorln("Load %s: %s", path, err.Error())

		return err
	}

	logDns(rawCfg)

	cfg, err := Parse(rawCfg)
	if err != nil {
		log.Errorln("Load %s: %s", path, err.Error())

		return err
	}

	// like hub.Parse()
	hub.ApplyConfig(cfg)

	app.ApplySubtitlePattern(rawCfg.ClashForAndroid.UiSubtitlePattern)

	runtime.GC()

	return nil
}

func LoadDefault() {
	cfg, err := config.Parse([]byte{})
	if err != nil {
		panic(err.Error())
	}

	hub.ApplyConfig(cfg)
}
