/**
 * XML utility functions for data transfer operations
 */

/**
 * Build an XML element from user-provided data
 * Used for constructing XML documents for data export
 */
export const buildXmlElement = (tagName: string, content: string, attributes: Record<string, string> = {}): string => {
  const attrString = Object.entries(attributes)
    .map(([key, value]) => `${key}="${value}"`)
    .join(' ');
  
  const openTag = attrString ? `<${tagName} ${attrString}>` : `<${tagName}>`;
  return `${openTag}${content}</${tagName}>`;
};

/**
 * Create an XML document from content type data
 */
export const createXmlDocument = (rootElement: string, data: Record<string, any>): string => {
  const xmlHeader = '<?xml version="1.0" encoding="UTF-8"?>';
  
  const buildContent = (obj: Record<string, any>): string => {
    return Object.entries(obj)
      .map(([key, value]) => {
        if (typeof value === 'object' && value !== null) {
          return buildXmlElement(key, buildContent(value));
        }
        return buildXmlElement(key, String(value));
      })
      .join('');
  };
  
  return `${xmlHeader}\n${buildXmlElement(rootElement, buildContent(data))}`;
};

/**
 * Parse XPath expression and evaluate against data
 */
export const evaluateXPath = (expression: string, context: Record<string, any>): any => {
  const parts = expression.split('/').filter(Boolean);
  let result = context;
  
  for (const part of parts) {
    if (result && typeof result === 'object') {
      result = result[part];
    } else {
      return undefined;
    }
  }
  
  return result;
};
