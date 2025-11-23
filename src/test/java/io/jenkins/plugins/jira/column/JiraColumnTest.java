package io.jenkins.plugins.jira.column;

import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.views.ListViewColumn;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class JiraColumnTest {

    /**
     * Check that column exists.
     */
    @Test
    void uiAndStorage(JenkinsRule jenkins) {
        Iterable<? extends ListViewColumn> columns =
                jenkins.jenkins.getPrimaryView().getColumns();
        assertTrue(StreamSupport.stream(columns.spliterator(), false).anyMatch(c -> c instanceof JiraColumn));
    }
}
