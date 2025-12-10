/**
 * XML parsing utilities for data import/export operations
 */
import { DOMParser } from '@xmldom/xmldom';

/**
 * Parse XML content from a string
 * Used for importing data from XML format
 */
export const parseXml = (xmlContent: string): Document => {
  const parser = new DOMParser();
  return parser.parseFromString(xmlContent, 'text/xml');
};

/**
 * Extract value from XML node by tag name
 */
export const getXmlNodeValue = (doc: Document, tagName: string): string | null => {
  const nodes = doc.getElementsByTagName(tagName);
  if (nodes.length > 0) {
    return nodes[0].textContent;
  }
  return null;
};

/**
 * Parse XML configuration file
 */
export const parseXmlConfig = (xmlContent: string): Record<string, any> => {
  const doc = parseXml(xmlContent);
  const result: Record<string, any> = {};
  
  const rootElement = doc.documentElement;
  if (rootElement) {
    const children = rootElement.childNodes;
    for (let i = 0; i < children.length; i++) {
      const child = children[i];
      if (child.nodeType === 1) {
        const element = child as Element;
        result[element.tagName] = element.textContent;
      }
    }
  }
  
  return result;
};

export default {
  parseXml,
  getXmlNodeValue,
  parseXmlConfig,
};
