/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package com.scireum;

import com.alibaba.fastjson.JSONObject;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.FileNotFoundException;

/**
 * Created by aha on 03.12.15.
 */
@Mojo(name = "verifyReleaseTag", defaultPhase = LifecyclePhase.VALIDATE)
public class VerifyReleaseTagMojo extends AbstractGithubTagMojo {

    @Parameter(property = "github.verifyReleaseTag.skip")
    private boolean skip;

    @Parameter(property = "project.version")
    private String version;

    @Override
    protected void executeWithConfig() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping (github.tag.verifyReleaseTag is true).");
            return;
        }
        if (version.endsWith("SNAPSHOT")) {
            getLog().info("Not going to verify a SNAPSHOT version. Skipping...");
            return;
        }
        try {
            JSONObject obj = call("GET", "/git/refs/tags/" + version, null);
            if (Integer.valueOf(200).equals(obj.get("state"))) {
                if (("refs/tags/" + version).equals(obj.get("ref"))) {
                    throw new MojoExecutionException("A release tag for "
                                                     + version
                                                     + " is already present! Aborting build");
                }
            }
        } catch (MojoExecutionException e) {
            throw e;
        } catch (FileNotFoundException e) {
            getLog().debug("Ignoring: " + e.getMessage());
        } catch (Exception e) {
            throw new MojoExecutionException("Cannot create tag: " + e.getMessage(), e);
        }
        getLog().info("Version " + version + " seems uneleased so far...");
    }
}
