package org.texttechnologylab.slc.io;

import org.apache.uima.UimaContext;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.dkpro.core.io.xmi.XmiWriter;

public class UriInfixXmiWriter extends XmiWriter {

    /**
     * Specify the infix of the to be added after the base URI.
     * Should not start or end with a slash, but can contain slashes in between.
     */
    public static final String PARAM_URI_INFIX = "uriInfix";
    @ConfigurationParameter(name = PARAM_URI_INFIX, mandatory = true)
    private String uriInfix;


    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);

        if (uriInfix.startsWith("/")) {
            uriInfix = uriInfix.substring(1);
        }
        if (uriInfix.endsWith("/")) {
            uriInfix = uriInfix.substring(0, uriInfix.length() - 1);
        }
        if (uriInfix.isEmpty()) {
            throw new ResourceInitializationException(new IllegalArgumentException("invalid uriInfix"));
        }
    }

    @Override
    protected String getRelativePath(JCas aJCas) {
        return uriInfix + "/" + super.getRelativePath(aJCas);
    }
}