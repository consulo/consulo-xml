package org.kohsuke.rngom.digested;

import org.kohsuke.rngom.ast.builder.GrammarSection;

/**
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public class DDefine {
    private final String name;
    private GrammarSection.Combine combine;

    private DPattern pattern;
    private Boolean nullable;
    DAnnotation annotation;

  public DDefine(String name) {
        this.name = name;
    }

    public DPattern getPattern() {
        return pattern;
    }

    public DAnnotation getAnnotation() {
        if(annotation==null)
            return DAnnotation.EMPTY;
        return annotation;
    }

    public void setPattern(DPattern pattern) {
        this.pattern = pattern;
        this.nullable = null;
    }

    /**
     * Gets the name of the pattern block.
     */
    public String getName() {
        return name;
    }

    public boolean isNullable() {
        if(nullable==null)
            nullable = pattern.isNullable()?Boolean.TRUE:Boolean.FALSE;
        return nullable.booleanValue();
    }

    public void setCombine(GrammarSection.Combine combine) {
      this.combine = combine;
    }

    public GrammarSection.Combine getCombine() {
      return combine;
    }
}
