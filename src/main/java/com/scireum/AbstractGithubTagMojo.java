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
import org.apache.maven.model.Scm;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base class for all goals provided by this plugin.
 * <p>
 * Takes care of fetching all necessarry parameters to use the GitHub API.
 */
public abstract class AbstractGithubTagMojo extends AbstractMojo {

    /**
     * Detects and parses scm urls like https://github.com/OWNER/REPO.git
     */
    private static final Pattern HTTP_GITHUB_URL = Pattern.compile("https?://github.com/([^/]+)/([^\\.]+)\\.git.*");

    /**
     * Detects and parses scm urls like git@git.scireum.local:OWNER/REPO.git
     */
    private static final Pattern SSH_GITHUB_URL = Pattern.compile("git@github\\.com:([^/]+)/([^\\.]+)\\.git.*");
    /**
     * Contains the oauth2 token used to access the GitHub API.
     * <p>
     * This should most probably be set in the settings.xml
     */
    @Parameter(property = "github.global.oauth2Token")
    protected String oauth2Token;

    /**
     * Contains the commit hash which might be tagged.
     */
    @Parameter(property = "github.commitHash")
    protected String commitHash;

    /**
     * Contains the repository owner.
     * <p>
     * If empty this will be filled by parsing the SCM URL
     */
    @Parameter(property = "github.repository.owner")
    protected String repositoryOwner;

    /**
     * Contains the repository name.
     * <p>
     * If empty this will be filled by parsing the SCM URL
     */
    @Parameter(property = "github.repository.name")
    protected String repositoryName;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    /**
     * Fetches the current commit.
     *
     * @return the current commit hash
     */
    protected String getCurrentCommit() {
        return commitHash;
    }

    /**
     * Returns the effective tag name containing the artifact id, as some repos contain multiple poms.
     *
     * @param tagName the base name of the tag
     * @return the fully qualified tag name <tt>artifactId-tagName</tt>
     */
    protected String getEffectiveTagName(String tagName) {
        return project.getArtifactId() + "-" + tagName;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (Strings.isNullOrEmpty(oauth2Token)) {
            getLog().info("No GitHub oauth2 token (github.global.oauth2Token) is present. Skipping....");
            return;
        }

        if (Strings.isNullOrEmpty(repositoryOwner)) {
            loadConfigFromSCM();

            if (Strings.isNullOrEmpty(repositoryOwner)) {
                getLog().info("No GitHub repository owner is present. Skipping....");
                return;
            }
        }

        if (Strings.isNullOrEmpty(repositoryName)) {
            getLog().info("No GitHub respository name is present. Skipping....");
            return;
        }

        executeWithConfig();
    }

    /*
     * Tries to parse repositoryOwner and repositoryName from the projects SCM URL
     */
    protected void loadConfigFromSCM() {
        final Scm scm = project.getScm();
        if (scm != null) {
            if (!Strings.isNullOrEmpty(scm.getUrl())) {
                Matcher m = HTTP_GITHUB_URL.matcher(scm.getUrl());
                if (m.matches()) {
                    repositoryOwner = m.group(1);
                    repositoryName = m.group(2);
                } else {
                    m = SSH_GITHUB_URL.matcher(scm.getUrl());
                    if (m.matches()) {
                        repositoryOwner = m.group(1);
                        repositoryName = m.group(2);
                    }
                }
            }
        }
    }

    /**
     * Executes a call against the GitHub API
     *
     * @param method the request method to use
     * @param uri    the relative url. <tt>https://api.github.com/repos/OWNER/REPO</tt> is already given
     * @param input  the JSON to send to the server. Use <tt>null</tt> for GET requests.
     * @return the parsed JSON from the server with <tt>state</tt> containing the HTTP status code
     * @throws Exception in case of any error
     */
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

    /**
     * Checks for a (probably) bad HTTP status code.
     *
     * @param obj the JSON response object
     * @return <tt>true</tt> if a non 200/201 status was found, <tt>false</tt> otherwise
     */
    protected boolean hasBadStatus(JSONObject obj) {
        return !Integer.valueOf(200).equals(obj.get("state")) && !Integer.valueOf(201).equals(obj.get("state"));
    }

    /**
     * Executes the goal with a valid config.
     *
     * @throws MojoExecutionException in case an error occurs
     */
    protected abstract void executeWithConfig() throws MojoExecutionException;
}
