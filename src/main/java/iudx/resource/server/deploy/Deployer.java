package iudx.resource.server.deploy;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import java.util.EnumSet;

import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import com.hazelcast.config.Config;
import com.hazelcast.config.DiscoveryStrategyConfig;
import com.hazelcast.zookeeper.*;

import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.cli.CLI;
import io.vertx.core.cli.Option;
import io.vertx.core.cli.CommandLine;

import iudx.resource.server.apiserver.ApiServerVerticle;
import iudx.resource.server.database.DatabaseVerticle;
import iudx.resource.server.databroker.DataBrokerVerticle;
import iudx.resource.server.callback.CallbackVerticle;
import iudx.resource.server.authenticator.AuthenticationVerticle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.vertx.core.metrics.MetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.Label;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.micrometer.backends.BackendRegistries;
// JVM metrics imports
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Deployer {
  private static final Logger LOGGER = LogManager.getLogger(Deployer.class);

  private static AbstractVerticle getVerticle(String name) {
    switch (name) {
      case "api":
        return new ApiServerVerticle();
      case "db":
        return new DatabaseVerticle();
      case "broker":
        return new DataBrokerVerticle();
      case "auth":
        return new AuthenticationVerticle();
      case "call":
        return new CallbackVerticle();
    }
    return null;
  }

  public static void recursiveDeploy(Vertx vertx, List<String> modules, int i) {
    if (i >= modules.size()) {
      LOGGER.info("Deployed all");
      return;
    }
    String moduleName = modules.get(i);
    vertx.deployVerticle(getVerticle(moduleName), ar -> {
      if (ar.succeeded()) {
        LOGGER.info("Deployed " + moduleName);
        recursiveDeploy(vertx, modules, i + 1);
      } else {
        LOGGER.fatal("Failed to deploy " + moduleName + " cause:", ar.cause());
      }
    });
  }

  public static ClusterManager getClusterManager(String host,
                                                  List<String> zookeepers,
                                                  String clusterID) {
    Config config = new Config();
    config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
    config.getNetworkConfig().setPublicAddress(host);
    config.setProperty("hazelcast.discovery.enabled", "true");
    config.setProperty("hazelcast.logging.type", "log4j2");
    DiscoveryStrategyConfig discoveryStrategyConfig =
        new DiscoveryStrategyConfig(new ZookeeperDiscoveryStrategyFactory());
    discoveryStrategyConfig.addProperty(ZookeeperDiscoveryProperties.ZOOKEEPER_URL.key(),
                                          String.join(",", zookeepers));
    discoveryStrategyConfig.addProperty(ZookeeperDiscoveryProperties.GROUP.key(), clusterID);
    config.getNetworkConfig()
          .getJoin()
          .getDiscoveryConfig()
          .addDiscoveryStrategyConfig(discoveryStrategyConfig);

    return new HazelcastClusterManager(config);
  }

  public static MetricsOptions getMetricsOptions() {
    return new MicrometerMetricsOptions()
        .setPrometheusOptions(
            new VertxPrometheusOptions().setEnabled(true).setStartEmbeddedServer(true)
                .setEmbeddedServerOptions(new HttpServerOptions().setPort(9000)))
        // .setPublishQuantiles(true))
        .setLabels(EnumSet.of(Label.EB_ADDRESS, Label.EB_FAILURE, Label.HTTP_CODE,
            Label.HTTP_METHOD, Label.HTTP_PATH))
        .setEnabled(true);
  }

  public static void setJVMmetrics() {
    MeterRegistry registry = BackendRegistries.getDefaultNow();
    new JvmMemoryMetrics().bindTo(registry);
    new JvmGcMetrics().bindTo(registry);
    new ProcessorMetrics().bindTo(registry);
    new JvmThreadMetrics().bindTo(registry);

  }

  public static void deploy(List<String> modules, List<String> zookeepers, String host) {
    ClusterManager mgr = getClusterManager(host, zookeepers, "iudx-rs");
    EventBusOptions ebOptions = new EventBusOptions().setClustered(true).setHost(host);
    VertxOptions options = new VertxOptions().setClusterManager(mgr).setEventBusOptions(ebOptions)
        .setMetricsOptions(getMetricsOptions());

    Vertx.clusteredVertx(options, res -> {
      if (res.succeeded()) {
        Vertx vertx = res.result();
        setJVMmetrics();
        recursiveDeploy(vertx, modules, 0);
      } else {
        LOGGER.fatal("Could not join cluster");
      }
    });

  }

  public static void main(String[] args) {
    CLI cli = CLI.create("IUDX RS").setSummary("A CLI to deploy the resource server")
        .addOption(new Option().setLongName("help").setShortName("h").setFlag(true)
            .setDescription("display help"))
        .addOption(new Option().setLongName("modules").setShortName("m").setMultiValued(true)
            .setRequired(true).setDescription("modules to launch").addChoice("api")
            .addChoice("db").addChoice("auth").addChoice("broker").addChoice("call"))
        .addOption(new Option().setLongName("zookeepers").setShortName("z").setMultiValued(true)
            .setRequired(true).setDescription("zookeeper hosts"))
        .addOption(new Option().setLongName("host").setShortName("i").setRequired(true)
            .setDescription("public host"));

    StringBuilder usageString = new StringBuilder();
    cli.usage(usageString);
    CommandLine commandLine = cli.parse(Arrays.asList(args), false);
    if (commandLine.isValid() && !commandLine.isFlagEnabled("help")) {
      List<String> modules = new ArrayList<String>(commandLine.getOptionValues("modules"));
      List<String> zookeepers = new ArrayList<String>(commandLine.getOptionValues("zookeepers"));
      String host = commandLine.getOptionValue("host");
      deploy(modules, zookeepers, host);
    } else {
      LOGGER.info(usageString);
    }
  }

}

