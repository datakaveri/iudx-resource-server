package iudx.resource.server.deploy;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

import io.vertx.core.eventbus.EventBusOptions;

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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * DeploySingle - Deploy a single non-clustered resource server instance
 **/
public class DeployerDev {
  private static final Logger LOGGER = LogManager.getLogger(DeployerDev.class);

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

  public static void deploy(List<String> modules) {
    EventBusOptions ebOptions = new EventBusOptions();
    VertxOptions options =
        new VertxOptions().setEventBusOptions(ebOptions).setPreferNativeTransport(true);
    Vertx vertx = Vertx.vertx(options);
    recursiveDeploy(vertx, modules, 0);
  }

  public static void main(String[] args) {
    CLI cli = CLI.create("IUDX Res").setSummary("A CLI to deploy the resource server")
        .addOption(new Option().setLongName("help").setShortName("h").setFlag(true)
            .setDescription("display help"))
        .addOption(new Option().setLongName("modules").setShortName("m").setMultiValued(true)
            .setRequired(true).setDescription("modules to launch").addChoice("api")
            .addChoice("db").addChoice("auth").addChoice("broker").addChoice("call"));

    StringBuilder usageString = new StringBuilder();
    cli.usage(usageString);
    CommandLine commandLine = cli.parse(Arrays.asList(args), false);
    if (commandLine.isValid() && !commandLine.isFlagEnabled("help")) {
      List<String> modules = new ArrayList<String>(commandLine.getOptionValues("modules"));
      deploy(modules);
    } else {
      LOGGER.info(usageString);
    }
  }
}

