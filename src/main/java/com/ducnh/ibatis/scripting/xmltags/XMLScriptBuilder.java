package com.ducnh.ibatis.scripting.xmltags;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ducnh.ibatis.builder.BaseBuilder;
import com.ducnh.ibatis.builder.BuilderException;
import com.ducnh.ibatis.mapping.SqlSource;
import com.ducnh.ibatis.parsing.XNode;
import com.ducnh.ibatis.reflection.ParamNameResolver;
import com.ducnh.ibatis.session.Configuration;

public class XMLScriptBuilder extends BaseBuilder{

	private final XNode context;
	private boolean isDynamic;
	private final Class<?> parameterType;
	private final ParamNameResolver paramNameResolver;
	private final Map<String, NodeHandler> nodeHandlerMap = new HashMap<>();
	private static final Map<String, SqlNode> emptyNodeCache = new ConcurrentHashMap<>();
	
	public XMLScriptBuilder(Configuration configuration, XNode context) {
		this(configuration, context, null);
	}
	
	public XMLScriptBuilder(Configuration configuration, XNode context, Class<?> parameterType) {
		this(configuration, context, parameterType, null);
	}
	
	public XMLScriptBuilder(Configuration configuration, XNode context, Class<?> parameterType,
		ParamNameResolver paramNameResolver) {
		super(configuration);
		this.context = context;
		this.parameterType = parameterType;
		this.paramNameResolver = paramNameResolver;
		initNodeHandlerMap();
	}
	
	private void initNodeHandlerMap() {
		nodeHandlerMap.put("trim", new TrimHandler());
		nodeHandlerMap.put("where", new WhereHandler());
	    nodeHandlerMap.put("set", new SetHandler());
	    nodeHandlerMap.put("foreach", new ForEachHandler());
	    nodeHandlerMap.put("if", new IfHandler());
	    nodeHandlerMap.put("choose", new ChooseHandler());
	    nodeHandlerMap.put("when", new IfHandler());
	    nodeHandlerMap.put("otherwise", new OtherwiseHandler());
	    nodeHandlerMap.put("bind", new BindHandler());
	}
	
	public SqlSource parseScriptNode() {
		MixedSqlNode rootSqlNode = parseDynamicTags(context);
		SqlSource sqlSource;
		if (isDynamic) {
			sqlSource = new DynamicSqlSource(configuration, rootSqlNode);
		} else {
			sqlSource = new RawSqlSource(configuration,.rootSqlNode, parameterType, paramNameResolver);
		}
		return sqlSource;
	}
	
	protected MixedSqlNode parseDynamicTags(XNode node) {
		List<SqlNode> contents = new ArrayList<>();
		NodeList children = node.getNode().getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			XNode child = node.newXNode(children.item(i));
			if (child.getNode().getNodeType() == Node.CDATA_SECTION_NODE || child.getNode().getNodeType() == Node.TEXT_NODE) {
				String data = child.getStringBody("");
				if (data.trim().isEmpty()) {
					contents.add(emptyNodeCache.computeIfAbsent(data, EmptySqlNode::new));
					continue;
				}
				TextSqlNode textSqlNode = new TextSqlNode(data);
				if (textSqlNode.isDynamic()) {
					contents.add(textSqlNode);
					isDynamic = true;
				} else {
					contents.add(new StaticTextSqlNode(data));
				}
			} else if (child.getNode().getNodeType() == Node.ELEMENT_NODE) {
				String nodeName = child.getNode().getNodeName();
				NodeHandler handler = nodeHandlerMap.get(nodeName);
				if (handler == null) {
					throw new BuilderException("Unknown element <" + nodeName + "> in SQL statement");
				}
				handler.handleNode(child, contents);
				isDynamic = true;
			}
		}
		return new MixedSqlNode(contents);
	}
	
	private interface NodeHandler {
		void handleNode(XNode nodeToHandler, List<SqlNode> targetContents);
	}
	
	private static class BindHandler implements NodeHandler {
		public BindHandler() {
			
		}
		
		@Override
		public void handleNode(XNode nodeToHandler, List<SqlNode> targetContents) {
			final String name = nodeToHandler.getStringAttribute("name");
			final String value = nodeToHandler.getStringAttribute("value");
			final VarDecSqlNode node = new VarDecSqlNode(name, value);
			targetContents.add(node);
		}
	}
	
	private class TrimHandler implements NodeHandler {
		public TrimHandler() {
			
		}
		
		@Override
		public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
			MixedSqlNode mixedSqlNode = parseDynamicTags(nodeToHandle);
			String prefix = nodeToHandle.getStringAttribute("prefix");
			String prefixOverrides = nodeToHandle.getStringAttribute("prefixOverrides");
			String suffix = nodeToHandle.getStringAttribute("suffix");
			String suffixOverrides = nodeToHandle.getStringAttribute("suffixOverrides");
			TrimSqlNode trim = new TrimSqlNode(configuration, mixedSqlNode, prefix, prefixOverrides, suffix, suffixOverrides);
			targetContents.add(trim);
		}
	}
	
	private class WhereHandler implements NodeHandler {
		public WhereHandler() {
			
		}
		
		@Override
		public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
			MixedSqlNode mixedSqlNode = parseDynamicTags(nodeToHandle);
			WhereSqlNode where = new WhereSqlNode(configuration, mixedSqlNode);
			targetContents.add(where); 
		}
	}
	
	private class SetHandler implements NodeHandler {
		public SetHandler() {
			
		}
		
		@Override
		public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
			MixedSqlNode mixedSqlNode = parseDynamicTags(nodeToHandle);
			SetSqlNode set = new SetSqlNode(configuration, mixedSqlNode);
			targetContents.add(set);
		}
	} 
	
	private class ForEachHandler implements NodeHandler {
		public ForEachHandler() {
			
		}
		
		@Override
		public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
			MixedSqlNode mixedSqlNode = parseDynamicTags(nodeToHandle);
			String collection = nodeToHandle.getStringAttribute("collection");
			Boolean nullable = nodeToHandle.getBooleanAttribute("nullable");
			String item = nodeToHandle.getStringAttribute("item");
			String index = nodeToHandle.getStringAttribute("index");
			String open = nodeToHandle.getStringAttribute("open");
			String close = nodeToHandle.getStringAttribute("close");
			String separator = nodeToHandle.getStringAttribute("separator");
			ForEachSqlNode forEachSqlNode = new ForEachSqlNode(configuration, mixedSqlNode, collection, nullable, index, item,
				open, close, separator);
			targetContents.add(forEachSqlNode);
		}
	}
	
	private class IfHandler implements NodeHandler {
		public IfHandler() {
			
		}
		
		@Override
		public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
			MixedSqlNode mixedSqlNode = parseDynamicTags(nodeToHandle);
			String test = nodeToHandle.getStringAttribute("test");
			IfSqlNode ifSqlNode = new IfSqlNode(mixedSqlNode, test);
			targetContents.add(ifSqlNode);
		}
	}
	
	private class OtherwiseHandler implements NodeHandler {
		public OtherwiseHandler() {
			
		}
		
		@Override
		public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
			MixedSqlNode mixedSqlNode = parseDynamicTags(nodeToHandle);
			targetContents.add(mixedSqlNode);
		}
	}
	
	private class ChooseHandler implements NodeHandler {
		public ChooseHandler() {
			
		}
		
		@Override
		public void handleNode(XNode nodeToHandle, List<SqlNode> targetContents) {
			List<SqlNode> whenSqlNodes = new ArrayList<>();
			List<SqlNode> otherwiseSqlNodes = new ArrayList<>();
			handleWhenOtherwiseNodes(nodeToHandle, whenSqlNodes, otherwiseSqlNodes);
			SqlNode defaultSqlNode = getDefaultSqlNode(otherwiseSqlNodes);
			ChooseSqlNode chooseSqlNode = new ChooseSqlNode(whenSqlNodes, defaultSqlNode);
			targetContents.add(chooseSqlNode);
		}
		
		private void handleWhenOtherwiseNodes(XNode chooseSqlNode, List<SqlNode> ifSqlNodes,
			List<SqlNode> defaultSqlNodes) {
			List<XNode> children = chooseSqlNode.getChildren();
			for (XNode child : children) {
				String nodeName = child.getNode().getNodeName();
				NodeHandler handler = nodeHandlerMap.get(nodeName);
				if (handler instanceof IfHandler) {
					handler.handleNode(child, ifSqlNodes);
				} else if (handler instanceof OtherwiseHandler) {
					handler.handleNode(child, defaultSqlNodes);
				} else {
					throw new BuilderException("Unknown element <" + nodeName + "> in SQL statement");
				}
			}
		}
		
		private SqlNode getDefaultSqlNode(List<SqlNode> defaultSqlNodes) {
			SqlNode defaultSqlNode = null;
			if (defaultSqlNodes.size() == 1) {
				defaultSqlNode = defaultSqlNodes.get(0);
			} else {
				throw new BuilderException("Too many default(otherwise) elements in choose statment.");
			}
			return defaultSqlNode;
		}
	}
	
	private static class EmptySqlNode implements SqlNode {
		private final String whitespaces;
		
		public EmptySqlNode(String whitespaces) {
			super();
			this.whitespaces = whitespaces;
		}
		
		@Override
		public boolean apply(DynamicContext context) {
			context.appendSql(whitespaces);
			return true;
		}
	}
}
