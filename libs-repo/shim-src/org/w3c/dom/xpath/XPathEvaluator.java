package org.w3c.dom.xpath;
import org.w3c.dom.Node;
public interface XPathEvaluator {
    XPathExpression createExpression(String expression, XPathNSResolver resolver) throws XPathException, org.w3c.dom.DOMException;
    XPathNSResolver createNSResolver(Node nodeResolver);
    Object evaluate(String expression, Node contextNode, XPathNSResolver resolver, short type, Object result) throws XPathException, org.w3c.dom.DOMException;
}
