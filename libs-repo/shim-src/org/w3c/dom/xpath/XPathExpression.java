package org.w3c.dom.xpath;
import org.w3c.dom.Node;
public interface XPathExpression {
    Object evaluate(Node contextNode, short type, Object result) throws XPathException, org.w3c.dom.DOMException;
}
