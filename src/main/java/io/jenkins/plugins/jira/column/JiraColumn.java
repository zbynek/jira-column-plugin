/*
The MIT License

Copyright (c) 2025 Jenkins contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

package io.jenkins.plugins.jira.column;

import com.atlassian.jira.rest.client.api.domain.Issue;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.model.TopLevelItem;
import hudson.plugins.jira.JiraSite;
import hudson.views.ListViewColumn;
import hudson.views.ListViewColumnDescriptor;
import org.jenkinsci.Symbol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Example of Jenkins global configuration.
 */
public class JiraColumn extends ListViewColumn {
    private static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
    private static final JiraStatus FALLBACK = new JiraStatus("", 0, "", "");
    private static final Pattern PATTERN = Pattern.compile("^([a-zA-Z]+-\\d+)(-.+)?$");
    private static final Map<String, JiraStatus> statuses = new HashMap<>();
    private static long lastRefresh = 0;
    private static final List<String> STATUS_ORDER = List.of(
        "", "To Do", "In Progress", "Ready for Review", "In Review", "In Main", "Rejected"
    );

    public ListViewColumnDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    public record JiraStatus(String name, int ordinal, String summary, String className) { }

    private static void refresh(Job<?, ?> job) {
        List<String> keys = new ArrayList<>();
        ItemGroup<? extends Item> iterable = ((TopLevelItem) job).getParent();
        iterable.allItems().forEach((Item item) -> {
            Matcher matcher = PATTERN.matcher(item.getName());
            if (matcher.find()) {
                keys.add(matcher.group(1).toUpperCase(Locale.ROOT));
            }
        });

        Optional.ofNullable(JiraSite.get(job)).map(s -> s.getSession(job)).ifPresent(session -> {
            try {
                List<Issue> issues = session.getIssuesFromJqlSearch(
                    "id in (" + String.join(",", keys) + ")");
                for (Issue issue : issues) {
                    String name = issue.getStatus().getName();
                    String className = List.of("In Main", "Rejected").contains(name) ? "warning" : "";
                    statuses.put(issue.getKey(), new JiraStatus(name,
                        STATUS_ORDER.indexOf(name), issue.getSummary(), className));
                }
            } catch (TimeoutException e) {
                // oh, well
            }
        });
    }

    public JiraStatus getJiraStatus(final Job<?, ?> job) {
        Matcher matcher = PATTERN.matcher(job.getName());
        if (!matcher.find()) {
            return FALLBACK;
        }
        if (statuses.isEmpty() || lastRefresh < System.currentTimeMillis() - 60_000) {
            lastRefresh = System.currentTimeMillis();
            refresh(job);
        }
        return statuses.getOrDefault(matcher.group(1).toUpperCase(Locale.ROOT), FALLBACK);
    }

    @Extension
    @Symbol({"jiraStatus"})
    public static class DescriptorImpl extends ListViewColumnDescriptor {
        @NonNull
        public String getDisplayName() {
            return "Jira Status";
        }
    }
}
