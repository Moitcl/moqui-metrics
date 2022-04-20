# Moqui Metrics

A module for [moqui-framework](https://github.com/moqui/moqui-framework) to make metrics available to monitoring tools, like [Prometheus](https://prometheus.io/).

## Supported Monitoring Tools

Currently, only Prometheus is supported. The URL to be used as target is the `/metrics/prometheus` path of your publically reachable moqui instance. If in your configuration the
public URL might reach more than one instance, you should add a unique name for each instance to be reached consistently, by e.g. using additional host names that are mapped to
only one instance.

## Configuration

This module currently has a setting to configure allowing access based on IP addresses using the `metrics_prometheus_servers` property.
This property can be set using java properties, environment vars or direct configuration in a `MoquiConf.xml` file. The value is a comma-separated list of hosts
(hostname or IP address), or the special value `private` which will match all IPv4 private address classes.

### Prometheus

#### Standalone Usage

1. Set the address(es) of the prometheus server as specified in the configuration
1. Configure prometheus to scrape the corresponding URL. For a prometheus server with a static configuration, something like this:
   ```
    scrape_configs:
      - job_name: 'External Moqui Instances'
        metrics_path: '/metrics/prometheus'
        scheme: https
        static_configs:
          - targets: [instance1.my-domain.com]
            labels:
              instance: 'instance1'
          - targets: [instance2.my-domain.com]
            labels:
              instance: 'instance1'
   ```

#### Kubernetes Usage

1. Make sure your prometheus server has endpoint services auto-discovery enabled (default for most installations)
2. Add the auto-discover labels to your moqui pods:
   * `prometheus.io/scrape=true`
   * `prometheus.io/path=/metrics/prometheus`
   * `prometheus.io/port=80` (or other port like 8080, according to your pod mapping)
