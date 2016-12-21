package io.katharsis.client.internal;

import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.katharsis.client.internal.proxy.ClientProxyFactory;
import io.katharsis.client.internal.proxy.ObjectProxy;
import io.katharsis.client.response.JsonLinksInformation;
import io.katharsis.client.response.JsonMetaInformation;
import io.katharsis.internal.boot.PropertiesProvider;
import io.katharsis.queryspec.internal.QueryAdapter;
import io.katharsis.resource.Document;
import io.katharsis.resource.Relationship;
import io.katharsis.resource.Resource;
import io.katharsis.resource.field.ResourceField;
import io.katharsis.resource.information.ResourceInformation;
import io.katharsis.resource.internal.DocumentMapper;
import io.katharsis.resource.internal.DocumentMapperUtil;
import io.katharsis.resource.internal.ResourceMapper;
import io.katharsis.resource.list.DefaultResourceList;
import io.katharsis.resource.registry.ResourceRegistry;
import io.katharsis.utils.PropertyUtils;
import io.katharsis.utils.java.Nullable;
import io.katharsis.utils.parser.TypeParser;

public class ClientDocumentMapper extends DocumentMapper {

	private ClientProxyFactory proxyFactory;

	private ObjectMapper objectMapper;

	private ResourceRegistry resourceRegistry;

	private TypeParser typeParser = new TypeParser();

	public ClientDocumentMapper(ResourceRegistry resourceRegistry, ObjectMapper objectMapper, PropertiesProvider propertiesProvider) {
		super(resourceRegistry, objectMapper, propertiesProvider, true);
		this.resourceRegistry = resourceRegistry;
		this.objectMapper = objectMapper;
	}

	@Override
	protected ResourceMapper newResourceMapper(final DocumentMapperUtil util, boolean client, ObjectMapper objectMapper) {
		return new ResourceMapper(util, client, objectMapper) {

			@Override
			protected void setRelationship(Resource resource, ResourceField field, Object entity, ResourceInformation resourceInformation, QueryAdapter queryAdapter) {
				// we also include relationship data if it is not null and not a
				// unloaded proxy
				boolean includeRelation = true;
				Object relationshipValue = PropertyUtils.getProperty(entity, field.getUnderlyingName());
				if (relationshipValue instanceof ObjectProxy) {
					includeRelation = ((ObjectProxy) relationshipValue).isLoaded();
				} else {
					// TODO for fieldSets handling in the future the lazy handling must be different
					includeRelation = relationshipValue != null || !field.isLazy() && !field.isCollection();
				}

				if(includeRelation){
					Relationship relationship = new Relationship();
					if(relationshipValue instanceof Collection){
						relationship.setData(Nullable.of((Object)util.toResourceIds((Collection<?>)relationshipValue)));
					}else{
						relationship.setData(Nullable.of((Object) util.toResourceId(relationshipValue)));
					}
					resource.getRelationships().put(field.getJsonName(), relationship);		
				}
			}
		};
	}

	public void setProxyFactory(ClientProxyFactory proxyFactory) {
		this.proxyFactory = proxyFactory;
	}

	public Object fromDocument(Document document, boolean getList) {
		ClientResourceUpsert upsert = new ClientResourceUpsert(resourceRegistry, typeParser, objectMapper, null, proxyFactory);

		if (document.getErrors() != null && !document.getErrors().isEmpty()) {

		}

		if (!document.getData().isPresent()) {
			return null;
		}

		List<Resource> included = document.getIncluded();
		List<Resource> data = document.getCollectionData().get();

		List<Object> dataObjects = upsert.allocateResources(data);
		if (included != null) {
			upsert.allocateResources(included);
		}

		upsert.setRelations(data);
		if (included != null) {
			upsert.setRelations(included);
		}

		if (getList) {
			DefaultResourceList<Object> resourceList = new DefaultResourceList();
			resourceList.addAll(dataObjects);
			if (document.getLinks() != null) {
				resourceList.setLinks(new JsonLinksInformation(document.getLinks(), objectMapper));
			}
			if (document.getMeta() != null) {
				resourceList.setMeta(new JsonMetaInformation(document.getMeta(), objectMapper));
			}
			return resourceList;
		} else {
			if(dataObjects.isEmpty()){
				return null;
			}
			if(dataObjects.size() != 1){
				throw new IllegalStateException("expected unique result " + dataObjects);
			}
			return dataObjects.get(0);
		}
	}

}
