/*
 * Made with all the love in the world
 * by scireum in Remshalden, Germany
 *
 * Copyright by scireum GmbH
 * http://www.scireum.de - info@scireum.de
 */

package com.scireum;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.io.CharStreams;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public abstract class AbstractGithubTagMojo extends AbstractMojo {

    @Parameter(property = "github.global.oauth2Token")
    protected String oauth2Token;

    @Parameter(property = "github.commitHash")
    protected String commitHash;

    @Parameter(defaultValue = "")
    protected String repositoryOwner;

    @Parameter(defaultValue = "")
    protected String repositoryName;

    protected String getCurrentCommit() {
        return commitHash;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (Strings.isNullOrEmpty(oauth2Token)) {
            getLog().info("No GitHub oauth2 token (github.global.oauth2Token) is present. Skipping....");
            return;
        }

        if (Strings.isNullOrEmpty(repositoryOwner)) {
            getLog().info("No GitHub repository owner is present. Skipping....");
            return;
        }

        if (Strings.isNullOrEmpty(repositoryName)) {
            getLog().info("No GitHub respository name is present. Skipping....");
            return;
        }

        executeWithConfig();
    }

    protected JSONObject call(String method, String uri, JSONObject input) throws Exception {
        URLConnection c = new URL("https://api.github.com/repos/"
                                  + repositoryOwner
                                  + "/"
                                  + repositoryName
                                  + uri
                                  + "?access_token="
                                  + oauth2Token).openConnection();
        ((HttpURLConnection) c).setRequestMethod(method);
        c.setDoInput(true);
        if (input != null) {
            c.setDoOutput(true);
            String inputAsJSON = input.toJSONString();
            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(c.getOutputStream(), Charsets.UTF_8))) {
                writer.print(inputAsJSON);
            }
        }
        String responseAsJSON = CharStreams.toString(new InputStreamReader(c.getInputStream(), Charsets.UTF_8));
        JSONObject result = JSON.parseObject(responseAsJSON);
        result.put("state", ((HttpURLConnection) c).getResponseCode());
        return result;
    }

    protected abstract void executeWithConfig() throws MojoExecutionException, MojoFailureException;
}
