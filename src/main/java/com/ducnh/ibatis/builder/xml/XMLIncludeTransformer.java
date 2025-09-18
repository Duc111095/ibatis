package com.ducnh.ibatis.builder.xml;

import java.util.Optional;
import java.util.Properties;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.ducnh.ibatis.builder.MapperBuilderAssistant;
import com.ducnh.ibatis.parsing.PropertyParser;
import com.ducnh.ibatis.session.Configuration;

public class XMLIncludeTransformer {

	private final Configuration configuration;
	private final MapperBuilderAssistant builderAssistant;
	
	public XMLIncludeTransformer(Configuration configuration, MapperBuilderAssistant builderAssistant) {
		this.configuration = configuration;
		this.builderAssistant = builderAssistant;
	}
	
	public void applyIncludes(Node source) {
		Properties variablesContext = new Properties();
		Properties configurationVariables = configuration.getVariables();
		Optional.ofNullable(configurationVariables).ifPresent(variablesContext::putAll);
		applyIncludes(source, variablesContext, false);
	}
	
	private void applyIncludes(Node source, final Properties variablesContext, boolean included) {
		if ("include".equals(source.getNodeName())) {
			Node toInclude = findSqlFragment(getStringAttribute(source, "refid"), variablesContext);
			Properties toIncludeContext = getVariablesContext(source, variablesContext);
			applyIncludes(toInclude, toIncludeContext, true);
			if (toInclude.getOwnerDocument() != source.getOwnerDocument()) {
				toInclude = source.getOwnerDocument().importNode(toInclude, true);
			}
			source.getParentNode().replaceChild(toInclude, source);
			while (toInclude.hasChildNodes()) {
				toInclude.getParentNode().insertBefore(toInclude.getFirstChild(), toInclude);
			}
			toInclude.getParentNode().removeChild(toInclude);
		} else if (source.getNodeType() == Node.ELEMENT_NODE) {
			if (included && !variablesContext.isEmpty()) {
				// replace variables in attribute values
				NamedNodeMap attributes = source.getAttributes();
				for (int i = 0; i < attributes.getLength(); i++) {
					Node attr = attributes.item(i);
					attr.setNodeValue(PropertyParser.parse(attr.getNodeValue(), variablesContext));
				}
			}
		}
	}
}
