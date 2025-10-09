package onemg.analytics.dump.service;

import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.*;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Comparator;



public class LocatorMatcher {
   

    public static boolean isLocatorPresent(String locator, String pageSource, ContextAnalyzerService.AutomationType pageType) {
        if (locator == null || pageSource == null || pageSource.isEmpty()) return false;

        try {
            // Case 1: XPath
            if (locator.startsWith("/") || locator.startsWith("./")) {
                return validateWithXPath(locator, pageSource);
            }

            // Case 2: Web locators
            if (pageType == ContextAnalyzerService.AutomationType.WEB) {
                return validateWebLocator(locator, pageSource);
            }

            // Case 3: Mobile locators
            if (pageType == ContextAnalyzerService.AutomationType.MOBILE_ANDROID || pageType == ContextAnalyzerService.AutomationType.MOBILE_IOS) {
                return validateMobileLocator(locator, pageSource);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // ✅ XPath evaluation for both Web + Mobile
    public static boolean validateWithXPath(String locator, String pageSource) {
        try{
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            org.w3c.dom.Document xmlDoc = builder.parse(
                new ByteArrayInputStream(pageSource.getBytes(StandardCharsets.UTF_8)));

        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xPath = xPathFactory.newXPath();

        XPathExpression expr = xPath.compile(locator);
        NodeList nodes = (NodeList) expr.evaluate(xmlDoc, XPathConstants.NODESET);
        
        return nodes.getLength() > 0;
        }
        catch(Exception e){

        }
        return false;
    }

        // ✅ Web-specific: id, name, css
    private static boolean validateWebLocator(String locator, String pageSource) {
        Document doc = Jsoup.parse(pageSource);

        // By ID
        Elements elements = doc.select("#" + locator);
        if (!elements.isEmpty()) return true;

        // By name
        elements = doc.select("[name=" + locator + "]");
        if (!elements.isEmpty()) return true;

        // Try CSS
        try {
            elements = doc.select(locator);
            if (!elements.isEmpty()) return true;
        } catch (Exception ignored) {}

        return false;
    }

    // ✅ Mobile-specific: resource-id, text, content-desc
    private static boolean validateMobileLocator(String locator, String pageSource) throws Exception {
        var builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        var xmlDoc = builder.parse(new ByteArrayInputStream(pageSource.getBytes(StandardCharsets.UTF_8)));
        XPath xPath = XPathFactory.newInstance().newXPath();

        // resource-id
        if (nodeExists(xPath, xmlDoc, "//*[@resource-id='" + locator + "']")) return true;

        // text
        if (nodeExists(xPath, xmlDoc, "//*[@text='" + locator + "']")) return true;

        // content-desc
        if (nodeExists(xPath, xmlDoc, "//*[@content-desc='" + locator + "']")) return true;

        return false;
    }

    private static boolean nodeExists(XPath xPath, org.w3c.dom.Document xmlDoc, String expr) throws Exception {
        XPathExpression expression = xPath.compile(expr);
        NodeList nodes = (NodeList) expression.evaluate(xmlDoc, XPathConstants.NODESET);
        return nodes.getLength() > 0;
    }


    /**
     * Return a valid XPath (present in the given pageSource) for the provided locator.
     * If locator is already a valid XPath and matches -> returned unchanged.
     * Otherwise attempts multiple wrapper XPaths (mobile & web styles) and returns the first match.
     *
     * @param locator    locator string (could be xpath, resource-id, id, class, text, tag, etc.)
     * @param pageSource HTML or XML page source
     * @return a matching XPath string, or null if none found
     */
    public static String ensureXPath(String locator, String pageSource) {
        if (locator == null || locator.trim().isEmpty() || pageSource == null || pageSource.isEmpty()) {
            return null;
        }
        locator = locator.trim();

        try {
            org.w3c.dom.Document doc = toW3cDocument(pageSource);
            XPath xpath = XPathFactory.newInstance().newXPath();

            // 1) If locator is already XPath and yields nodes -> return as-is
            try {
                XPathExpression checkExpr = xpath.compile(locator);
                NodeList checkNodes = (NodeList) checkExpr.evaluate(doc, XPathConstants.NODESET);
                if (checkNodes != null && checkNodes.getLength() > 0) {
                    return locator;
                }
            } catch (XPathExpressionException ignored) {
                // not a valid/parsable XPath -> we'll wrap
            }

            // 2) Decide whether doc looks like mobile XML (presence of resource-id attr)
            boolean looksLikeMobile = docHasAttribute(doc, "resource-id");

            // 3) Build candidate xpaths
            List<String> candidates = new ArrayList<>();

            // If locator itself looks like an absolute/relative xpath string (starts with / or (// or .//) but did not match earlier)
            // we still add a safe wrapper: search any node whose @resource-id or text contains that substring
            // (but only if locator is not obviously xpath compileable)
            // -> skip here, below we add attribute-based candidates.

            // MOBILE-style candidate wrappers
            String lit = xpathStringLiteral(locator);
            String litLower = xpathStringLiteral(locator.toLowerCase(Locale.ROOT));

            if (looksLikeMobile) {
                // exact resource-id
                candidates.add("//*[@" + "resource-id" + "=" + lit + "]");
                // contains resource-id
                candidates.add("//*[contains(@resource-id," + lit + ")]");

                // exact text
                candidates.add("//*[@" + "text" + "=" + lit + "]");
                // case-insensitive contains on @text
                candidates.add("//*[contains(translate(normalize-space(@text),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'), " + litLower + ")]");

                // content-desc
                candidates.add("//*[@" + "content-desc" + "=" + lit + "]");
                candidates.add("//*[contains(@content-desc," + lit + ")]");

                // fallback: any attribute contains locator (generic)
                candidates.add("//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'), " + litLower + ")]");
            }

            // WEB-style candidate wrappers (also safe for XML if not mobile)
            // tag (if locator is a simple tag name like 'div' or 'span')
            if (locator.matches("^[A-Za-z0-9_-]+$")) {
                candidates.add("//" + locator);
            }

            // id attribute
            candidates.add("//*[@id=" + lit + "]");
            // name
            candidates.add("//*[@name=" + lit + "]");
            // exact normalized text node (normalize-space(.))
            candidates.add("//*[normalize-space(text())=" + lit + "]");
            // case-insensitive contains on element text
            candidates.add("//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'), " + litLower + ")]");

            // class token match (single token). safe pattern using concat trick
            // matches when class attribute contains that token as a CSS class
            candidates.add("//*[contains(concat(' ', normalize-space(@class), ' '), concat(' '," + lit + ", ' '))]");

            // If locator looks like 'tag[@attr=..]' (e.g., div[@class='x']) it's probably XPath-ish; try it as an XPath
            if (looksXpathLike(locator)) {
                candidates.add(locator); // try once more (if earlier failed, this may still be OK)
            }

            // 4) Evaluate candidates in order and return the first that matches
            for (String cand : candidates) {
                try {
                    XPathExpression expr = xpath.compile(cand);
                    NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
                    if (nodes != null && nodes.getLength() > 0) {
                        return cand;
                    }
                } catch (XPathExpressionException ignore) {
                    // skip invalid candidate
                }
            }

        } catch (Exception e) {
            // parsing/evaluation error -> return null (or rethrow as needed)
            e.printStackTrace();
        }
        return null;
    }

    public static HashMap<String,Object> healLocator(String locator, String pageSource) {
        HashMap<String,Object> result = new HashMap<>();
        result.put("originalLocator", locator);
        if (locator == null || pageSource == null || locator.trim().isEmpty() || pageSource.isEmpty()) {
            result.put("healedXPath", locator);
            result.put("confidence", 0.0);
            return result;
        }
        try {
            org.w3c.dom.Document doc = toW3cDocument(pageSource);
            XPath xp = XPathFactory.newInstance().newXPath();

            // 1) If locator already XPath and matches -> return it
            try {
                XPathExpression expr = xp.compile(locator);
                NodeList matches = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
                if (matches != null && matches.getLength() > 0) {
                    result.put("healedXPath", locator);
                    result.put("confidence", 1.0);
                    return result;
                }
            } catch (XPathExpressionException ignore) { /* not valid xpath */ }

            // 2) Extract tokens: resource-id in xpath, plain resource-id, text value, id attribute
            String resourceId = extractRegex(locator, "@resource-id\\s*=\\s*(['\"])(.*?)\\1");
            if (resourceId == null) resourceId = extractRegex(locator, "([\\w\\.]+:id/[\\w_\\-]+)");
            String idAttr = extractRegex(locator, "@id\\s*=\\s*(['\"])(.*?)\\1");
            String textVal = extractRegex(locator, "text\\(\\)\\s*=\\s*(['\"])(.*?)\\1");
            if (textVal == null) textVal = extractRegex(locator, "@text\\s*=\\s*(['\"])(.*?)\\1");
            if (textVal == null) textVal = extractQuotedFreeText(locator);

            // 3) Try exact resource-id match -> highest confidence
            if (resourceId != null) {
                String expr = "//*[@resource-id=" + xpathStringLiteral(resourceId) + "]";
                if (nodesExist(xp, doc, expr)) {
                    result.put("healedXPath", expr);
                    result.put("confidence", 1.0);
                    return result;
                }
                // contains resource-id
                expr = "//*[contains(@resource-id," + xpathStringLiteral(resourceId) + ")]";
                if (nodesExist(xp, doc, expr)) {
                    result.put("healedXPath", expr);
                    result.put("confidence", 0.9);
                    return result;
                }
            }

            // 4) Try id attribute
            if (idAttr != null) {
                String expr = "//*[@id=" + xpathStringLiteral(idAttr) + "]";
                if (nodesExist(xp, doc, expr)) {
                    result.put("healedXPath", expr);
                    result.put("confidence", 1.0);
                    return result;
                }
            }

            // 5) If text-like target, fuzzy search over nodes' text/@text
            if (textVal != null && !textVal.trim().isEmpty()) {
                String targetNorm = normalize(textVal);
                NodeList textNodes = (NodeList) xp.compile("//*[normalize-space(@text) or normalize-space(text())]").evaluate(doc, XPathConstants.NODESET);
                double bestScore = 0.0;
                Node bestNode = null;
                for (int i = 0; i < textNodes.getLength(); i++) {
                    Node n = textNodes.item(i);
                    String candidate = "";
                    NamedNodeMap attrs = n.getAttributes();
                    if (attrs != null && attrs.getNamedItem("text") != null) candidate = attrs.getNamedItem("text").getNodeValue();
                    if (candidate.isEmpty()) candidate = n.getTextContent();
                    String candNorm = normalize(candidate);
                    if (candNorm.isEmpty()) continue;
                    double sim = similarity(targetNorm, candNorm);
                    if (sim > bestScore) { bestScore = sim; bestNode = n; }
                }
                if (bestNode != null && bestScore > 0.25) { // threshold, tuneable
                    // Prefer resource-id on matched node for stable xpath
                    String rid = (bestNode.getAttributes() != null && bestNode.getAttributes().getNamedItem("resource-id") != null)
                            ? bestNode.getAttributes().getNamedItem("resource-id").getNodeValue() : null;
                    String id = (bestNode.getAttributes() != null && bestNode.getAttributes().getNamedItem("id") != null)
                            ? bestNode.getAttributes().getNamedItem("id").getNodeValue() : null;
                    String healed;
                    if (rid != null && !rid.isEmpty()) {
                        healed = "//*[@resource-id=" + xpathStringLiteral(rid) + "]";
                    } else if (id != null && !id.isEmpty()) {
                        healed = "//*[@id=" + xpathStringLiteral(id) + "]";
                    } else {
                        // case-insensitive contains on node text
                        healed = "//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'), " +
                                xpathStringLiteral(targetNorm) + ")]";
                    }
                    // Confidence = clamp(bestScore, 0.0, 1.0)
                    double confidence = Math.max(0.0, Math.min(1.0, bestScore));
                    result.put("healedXPath", healed);
                    result.put("confidence", confidence);
                    return result;
                }
            }

        } catch (Exception e) {
            // swallow and fall-through to return 0.0 result
        }

        // Nothing found -> return same locator with confidence 0.0
        result.put("healedXPath", locator);
        result.put("confidence", 0.0);
        return result;
    }

    // --- Helpers ---

    // Convert pageSource to W3C DOM Document:
    private static org.w3c.dom.Document toW3cDocument(String pageSource) throws Exception {
        // try XML parse first (works for Android / iOS page sources)
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(false);
            // Prevent XXE
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return (org.w3c.dom.Document) dbf.newDocumentBuilder().parse(new ByteArrayInputStream(pageSource.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception xmlEx) {
            // fallback: parse as HTML with Jsoup and convert to W3C DOM
            org.jsoup.nodes.Document jsoupDoc = Jsoup.parse(pageSource);
            W3CDom w3cDom = new W3CDom();
            return (org.w3c.dom.Document) w3cDom.fromJsoup(jsoupDoc);
        }
    }

    // Detect if a DOM contains at least one node with the given attribute name
    private static boolean docHasAttribute(org.w3c.dom.Document doc, String attribute) {
        try {
            XPath xp = XPathFactory.newInstance().newXPath();
            String expr = "//*[@" + attribute + "]";
            XPathExpression xpe = xp.compile(expr);
            NodeList nl = (NodeList) xpe.evaluate(doc, XPathConstants.NODESET);
            return nl != null && nl.getLength() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    // Construct a safe XPath string literal for arbitrary input (handles single/double quotes via concat(...) if needed)
    private static String xpathStringLiteral(String s) {
        if (!s.contains("'")) {
            return "'" + s + "'";
        }
        if (!s.contains("\"")) {
            return "\"" + s + "\"";
        }
        // contains both single and double quotes -> use concat('a',"'",'b',...)
        String[] parts = s.split("'");
        StringBuilder sb = new StringBuilder("concat(");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                sb.append(", \"'\", ");
            }
            sb.append("'").append(parts[i]).append("'");
        }
        sb.append(")");
        return sb.toString();
    }

    // quick heuristic: does locator "look like" an xpath?
    private static boolean looksXpathLike(String locator) {
        String t = locator.trim();
        return t.startsWith("/") || t.startsWith("(") || t.contains("::") || t.contains("()")
                || t.contains("@") || t.contains("text()") || t.contains("contains(") || t.contains("normalize-space(")
                || t.startsWith(".//") || t.startsWith("//*");
    }

    private static boolean nodesExist(XPath xp, org.w3c.dom.Document doc, String expr) {
        try {
            NodeList nl = (NodeList) xp.compile(expr).evaluate(doc, XPathConstants.NODESET);
            return nl != null && nl.getLength() > 0;
        } catch (Exception e) { return false; }
    }

    private static String extractQuotedFreeText(String s) {
        Matcher m = Pattern.compile("(['\"])([^'\"]{2,})\\1").matcher(s);
        return m.find() ? m.group(2) : null;
    }

        private static String normalize(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}\\s]+", " ").trim().replaceAll("\\s+", " ");
    }

    private static double similarity(String a, String b) {
        if (a.equals(b)) return 1.0;
        int dist = levenshtein(a, b);
        int max = Math.max(a.length(), b.length());
        return max == 0 ? 1.0 : Math.max(0.0, 1.0 - (double) dist / max);
    }

    private static int levenshtein(String s1, String s2) {
        int[] prev = new int[s2.length() + 1];
        for (int j = 0; j <= s2.length(); j++) prev[j] = j;
        for (int i = 1; i <= s1.length(); i++) {
            int[] cur = new int[s2.length() + 1];
            cur[0] = i;
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            prev = cur;
        }
        return prev[s2.length()];
    }


    private static String extractRegex(String s, String regex) {
        Matcher m = Pattern.compile(regex).matcher(s);
        return m.find() ? (m.groupCount() >= 2 ? m.group(2) : m.group(1)) : null;
    }

}
