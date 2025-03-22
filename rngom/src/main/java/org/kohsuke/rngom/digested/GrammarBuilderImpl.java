package org.kohsuke.rngom.digested;

import org.kohsuke.rngom.ast.builder.*;
import org.kohsuke.rngom.ast.om.Location;
import org.kohsuke.rngom.ast.om.ParsedElementAnnotation;
import org.kohsuke.rngom.ast.om.ParsedPattern;
import org.kohsuke.rngom.ast.util.LocatorImpl;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
class GrammarBuilderImpl implements Grammar, Div {

    protected final DGrammarPattern grammar;

    protected final Scope parent;

    protected final DSchemaBuilderImpl sb;

    /**
     * Additional top-level element annotations.
     * Can be null.
     */
    private List<Element> additionalElementAnnotations;

    public GrammarBuilderImpl(DGrammarPattern p, Scope parent, DSchemaBuilderImpl sb) {
        this.grammar = p;
        this.parent = parent;
        this.sb = sb;
    }

    public ParsedPattern endGrammar(Location loc, Annotations anno) throws BuildException {
        if(anno!=null)
            grammar.annotation = ((Annotation)anno).getResult();
        if(additionalElementAnnotations!=null) {
            if(grammar.annotation==null)
                grammar.annotation = new DAnnotation();
            if (grammar.annotation.contents == null) {
                grammar.annotation.contents = new ArrayList<Element>();
            }

            grammar.annotation.contents.addAll(additionalElementAnnotations);
        }
        return grammar;
    }

    public void endDiv(Location loc, Annotations anno) throws BuildException {
    }

    public void define(String name, Combine combine, ParsedPattern pattern, Location loc, Annotations anno) throws BuildException {
        if(name==START)
            grammar.start = (DPattern)pattern;
        else {
          // TODO: handle combine - better?
          DDefine d = grammar.getOrAdd(name);
          if (d.getPattern() == null) {
            d.setPattern( (DPattern) pattern );
            d.setCombine(combine);
          } else if (combine == COMBINE_CHOICE) {
            d.setPattern(sb.makeChoice(Arrays.asList((DPattern)pattern, d.getPattern()), null, null));
          } else if (combine == COMBINE_INTERLEAVE) {
            d.setPattern(sb.makeInterleave(Arrays.asList((DPattern)pattern, d.getPattern()), null, null));
          } else {
            combine = d.getCombine();
            d.setCombine(null);
            if (combine == COMBINE_CHOICE) {
              d.setPattern(sb.makeChoice(Arrays.asList((DPattern)pattern, d.getPattern()), null, null));
            } else if (combine == COMBINE_INTERLEAVE) {
              d.setPattern(sb.makeInterleave(Arrays.asList((DPattern)pattern, d.getPattern()), null, null));
            } else {
              // dunno, this is an error
//              d.setPattern(sb.makeErrorPattern());
            }
          }
          if (anno != null) {
            d.annotation = ((Annotation)anno).getResult();
          }
        }
    }

    public void topLevelAnnotation(ParsedElementAnnotation ea) throws BuildException {
        if(additionalElementAnnotations==null)
            additionalElementAnnotations = new ArrayList<Element>();
        additionalElementAnnotations.add(((ElementWrapper)ea).element);
    }

    public void topLevelComment(CommentList comments) throws BuildException {
    }

    public Div makeDiv() {
        return this;
    }

    public Include makeInclude() {
        return new IncludeImpl(grammar,parent,sb);
    }

    public ParsedPattern makeParentRef(String name, Location loc, Annotations anno) throws BuildException {
        return parent != null ? parent.makeRef(name,loc,anno) :
                DSchemaBuilderImpl.wrap( new DRefPattern(new DDefine(name)), (LocatorImpl)loc, (Annotation)anno );
    }

    public ParsedPattern makeRef(String name, Location loc, Annotations anno) throws BuildException {
        return DSchemaBuilderImpl.wrap( new DRefPattern(grammar.getOrAdd(name)), (LocatorImpl)loc, (Annotation)anno );
    }
}
