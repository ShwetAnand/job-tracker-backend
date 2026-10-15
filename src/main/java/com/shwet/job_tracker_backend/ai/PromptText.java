package com.shwet.job_tracker_backend.ai;

/** Helpers for placing untrusted text (resumes, job descriptions) inside prompts. */
public final class PromptText {

    private PromptText() {
    }

    /**
     * Neutralises angle brackets so untrusted text cannot close or forge the XML-style
     * delimiters (&lt;resume&gt;, &lt;job_description&gt;, ...) that separate data from instructions.
     */
    public static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("<", "&lt;").replace(">", "&gt;");
    }
}
