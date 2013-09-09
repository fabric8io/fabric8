package org.fusesource.camel.component.sap.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import junit.framework.Assert;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EGenericType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.ETypeParameter;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceImpl;
import org.fusesource.camel.component.sap.model.rfc.DestinationData;
import org.fusesource.camel.component.sap.model.rfc.DestinationDataStore;
import org.fusesource.camel.component.sap.model.rfc.RfcFactory;
import org.fusesource.camel.component.sap.model.rfc.RfcPackage;
import org.fusesource.camel.component.sap.model.rfc.Structure;
import org.fusesource.camel.component.sap.model.rfc.Table;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoFunctionTemplate;
import com.sap.conn.jco.JCoRecordMetaData;
import com.sap.conn.jco.JCoRepository;

public class RfcUtilTest {

	protected DestinationDataStore destinationDataStore;

	@Before
	public void setUp() throws Exception {
		
		DestinationData destinationData = RfcFactory.eINSTANCE.createDestinationData();
		destinationData.setAshost("nplhost");
		destinationData.setSysnr("42");
		destinationData.setClient("001");
		destinationData.setUser("developer");
		destinationData.setPasswd("ch4ngeme");
		destinationData.setLang("en");
		
		destinationDataStore = RfcFactory.eINSTANCE.createDestinationDataStore();
		destinationDataStore.getEntries().put("TestDestination", destinationData);
		
		ComponentDestinationDataProvider.INSTANCE.addDestinationDataStore(destinationDataStore);
	}

	@After
	public void tearDown() throws Exception {
		//ComponentDestinationDataProvider.INSTANCE.removeDestinationDataStore(destinationDataStore);
	}

	//@Test
	public void test() throws JCoException {
		JCoDestination jcoDestination = JCoDestinationManager.getDestination("TestDestination"); 
		
		Structure request = RfcUtil.getRequest(jcoDestination.getRepository(), "STFC_CONNECTION");
		RfcUtil.setValue(request, "REQUTEXT", "Hello, SAP!");
		
		Structure response = RfcUtil.executeFunction(jcoDestination, "STFC_CONNECTION", request);
		
		String echoText = (String) RfcUtil.getValue(response, "ECHOTEXT");
		String respText = (String) RfcUtil.getValue(response, "RESPTEXT");
		
		Assert.assertEquals("ECHOTEXT of response different from REQUTEXT of request", "Hello, SAP!", echoText);
		System.out.println("RESPTEXT: " + respText);
	}
	
	//@Test
	public void testMashalling() throws Exception {
		JCoDestination jcoDestination = JCoDestinationManager.getDestination("TestDestination");
		
		Structure customerData = (Structure) RfcUtil.getInstance(jcoDestination.getRepository(), "BAPI_FLCUST_GETLIST", "BAPISCUDAT");
		EList<EStructuralFeature> list = customerData.eClass().getEAllStructuralFeatures();
		for(EStructuralFeature f: list) {
			System.out.println(f);
		}
		
		Structure request = RfcUtil.getRequest(jcoDestination.getRepository(), "BAPI_FLCUST_GETLIST");
		Table<?> table = (Table<?>) RfcUtil.getValue(request, "CUSTOMER_LIST");
		System.out.println("Table type name: " + table.eClass().getName());
		System.out.println("Table type Generic SuperTypes: " + table.eClass().getEGenericSuperTypes());
		EGenericType genericSuperType = table.eClass().getEGenericSuperTypes().get(0);
		System.out.println("Generic SuperType Type Arguments: " + genericSuperType.getETypeArguments());
		EGenericType typeArgument = genericSuperType.getETypeArguments().get(0);
		System.out.println("Type Argument Classifier: " + typeArgument.getEClassifier());
		
		EStructuralFeature feature = table.eClass().getEStructuralFeature("row");
		EClass rowType = ((EReference) feature).getEReferenceType();
		System.out.println("Row Type: " + rowType);
		System.out.println("Row Generic Type" + ((EReference) feature).getEGenericType());
		list = rowType.getEAllStructuralFeatures();
		for(EStructuralFeature f: list) {
			System.out.println(f);
		}
		@SuppressWarnings("unused")
		Structure newRow = (Structure) rowType.getEPackage().getEFactoryInstance().create(rowType);
		
		EClass tableClass = RfcPackage.eINSTANCE.getTable();
		System.out.println("Table class: " + tableClass);
		EList<ETypeParameter> tableClassTypeParameters = tableClass.getETypeParameters();
		System.out.println("Table class type parameters " + tableClassTypeParameters);
		@SuppressWarnings("unused")
		ETypeParameter typeParameter = tableClassTypeParameters.get(0);
	}
	
	//@Test
	public void testCreateTableSubclass() throws Exception {
		JCoDestination jcoDestination = JCoDestinationManager.getDestination("TestDestination");
		
		JCoRepository jcoRepository = jcoDestination.getRepository();
		JCoFunctionTemplate functionTemplate = jcoRepository.getFunctionTemplate("BAPI_FLCUST_GETLIST");
		JCoRecordMetaData jcoRecordMetaData = functionTemplate.getTableParameterList().getRecordMetaData("CUSTOMER_LIST");

		// Create and initialize package
		EcoreFactory ecoreFactory = EcoreFactory.eINSTANCE;
		EPackage ePackage = ecoreFactory.createEPackage();
		ePackage.setName("BAPI_FLCUST_GETLIST");
		ePackage.setNsPrefix("BAPI_FLCUST_GETLIST");
		ePackage.setNsURI("http://sap.fusesource.org/rfc/NPL/BAPI_FLCUST_GETLIST");
		
		// Create the super type inherited by this Table subclass: i.e.
		// 'Table<S extends Structure>'
		EGenericType tableGenericSuperType = EcoreFactory.eINSTANCE.createEGenericType();
		EClass tableSuperClass = RfcPackage.eINSTANCE.getTable();
		tableGenericSuperType.setEClassifier(tableSuperClass);

		// Create type parameter for row type: i.e. the 'S' in 'S extends
		// Structure'
		EGenericType rowGenericType = EcoreFactory.eINSTANCE.createEGenericType();
		EClass structureType = RfcUtil.getStructureClass(ePackage, jcoRecordMetaData);
		rowGenericType.setEClassifier(structureType);
		
		// Add the type parameter to super type: i.e. 'S'
		tableGenericSuperType.getETypeArguments().add(rowGenericType);

		// Create the Table subclass and add to package
		EClass tableClass = EcoreFactory.eINSTANCE.createEClass();
		ePackage.getEClassifiers().add(tableClass);
		tableClass.setName(jcoRecordMetaData.getName() + "_TABLE");
		((EClass) tableClass).getEGenericSuperTypes().add(tableGenericSuperType);

        Resource res = new XMLResourceImpl();
        res.getContents().add(ePackage);
        res.save(System.out, null);
        
        // Test instance of table class
		EObject eObject = ePackage.getEFactoryInstance().create(tableClass);
		EStructuralFeature rowFeature = eObject.eClass().getEStructuralFeature("row");
		System.out.println("Row feature: " + rowFeature);
        System.out.println("Row feature type: " + rowFeature.getEType());
        System.out.println("Row feature generic type: " + rowFeature.getEGenericType());
		
	}
	
	//@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void testApplesAndOranges() throws Exception {
		// Create and initialize package
		EcoreFactory ecoreFactory = EcoreFactory.eINSTANCE;
		EPackage ePackage = ecoreFactory.createEPackage();
		ePackage.setName("fruit");
		ePackage.setNsPrefix("fruit");
		ePackage.setNsURI("http://fruit");
		
		// Create Fruit class
		EClass fruitClass = ecoreFactory.createEClass();
		ePackage.getEClassifiers().add(fruitClass);
		fruitClass.setName("Fruit");
		
		// Create Apple class
		EClass appleClass = ecoreFactory.createEClass();
		ePackage.getEClassifiers().add(appleClass);
		appleClass.setName("Apple");
		appleClass.getESuperTypes().add(fruitClass);
		
		// Create Orange class
		EClass orangeClass = ecoreFactory.createEClass();
		ePackage.getEClassifiers().add(orangeClass);
		orangeClass.setName("Orange");
		orangeClass.getESuperTypes().add(fruitClass);
		
		// Create Basket class
		//
		EClass basketClass = ecoreFactory.createEClass();
		ePackage.getEClassifiers().add(basketClass);
		basketClass.setName("Basket");
		
		EGenericType fTypeParamterBounds = ecoreFactory.createEGenericType();
		fTypeParamterBounds.setEClassifier(fruitClass);
		
		ETypeParameter fTypeParameter = ecoreFactory.createETypeParameter();
		fTypeParameter.setName("F");
		fTypeParameter.getEBounds().add(fTypeParamterBounds);
		
		basketClass.getETypeParameters().add(fTypeParameter);
		
		EReference contentsFeature = ecoreFactory.createEReference();
		contentsFeature.setName("contents");
		contentsFeature.setLowerBound(1);
		contentsFeature.setUpperBound(-1);
		
		EGenericType contentsGenericType = ecoreFactory.createEGenericType();
		contentsGenericType.setETypeParameter(fTypeParameter);
		contentsFeature.setEGenericType(contentsGenericType);
		
		basketClass.getEStructuralFeatures().add(contentsFeature);
		
		//
		// End create Basket class
		
		// Create AppleBasket class
		//
		
		EClass appleBasketClass = ecoreFactory.createEClass();
		ePackage.getEClassifiers().add(appleBasketClass);
		appleBasketClass.setName("AppleBasket");
		
		EGenericType appleBasketGenericSuperType = ecoreFactory.createEGenericType();
		appleBasketGenericSuperType.setEClassifier(basketClass);
		
		EGenericType appleBasketTypeArgument = ecoreFactory.createEGenericType();
		appleBasketTypeArgument.setEClassifier(appleClass);
		
		appleBasketGenericSuperType.getETypeArguments().add(appleBasketTypeArgument);
		
		appleBasketClass.getEGenericSuperTypes().add(appleBasketGenericSuperType);
		
		// Workaround for type erasure in feature.
		EReference rowReference = ecoreFactory.createEReference();
		rowReference.setEType(appleClass);
		rowReference.setName("contents");
		rowReference.setContainment(true);
		rowReference.setLowerBound(0);
		rowReference.setUpperBound(-1);
		appleBasketClass.getEStructuralFeatures().add(rowReference);
		
		//
		// End Create AppleBasket class
		
		
        Resource res = new XMLResourceImpl();
        res.getContents().add(ePackage);
        res.save(System.out, null);
	
        // Now test instance of appleBasket.
        EObject appleBasket = ePackage.getEFactoryInstance().create(appleBasketClass);
        EStructuralFeature feature = appleBasket.eClass().getEStructuralFeature("contents");
        System.out.println("Contents Feature: " + feature);
        System.out.println("Contents Feature Type: " + feature.getEType());

        // Test fruit type of AppleBasket
        EObject apple = ePackage.getEFactoryInstance().create(appleClass);
        System.out.println("Apple instance: " + apple);
        EObject orange = ePackage.getEFactoryInstance().create(orangeClass);
        System.out.println("Orange instance: " + orange);
        @SuppressWarnings("unused")
		EList contents = (EList) appleBasket.eGet(feature);
        System.out.println("Apple basket contents: " + appleBasket.eGet(feature));
        ((EList)appleBasket.eGet(feature)).add(apple);
        System.out.println("Apple basket contents: " + appleBasket.eGet(feature));
        if (feature.getEType().isInstance(orange)) {
        	System.out.println("Orange is instance of feature type");
        } else {
        	System.out.println("Orange is NOT instance of feature type");
        }
        if (feature.getEType().isInstance(apple)) {
        	System.out.println("Apple is instance of feature type");
        } else {
        	System.out.println("Apple is NOT instance of feature type");
        }
        ((EList)appleBasket.eGet(feature)).add(orange);
        System.out.println("Apple basket contents: " + appleBasket.eGet(feature));
        
	}
	
	//@Test
	public void testPackage() throws Exception {
		JCoDestination jcoDestination = JCoDestinationManager.getDestination("TestDestination");
		EPackage ePackage = RfcUtil.getEPackage(jcoDestination.getRepository(), "http://sap.fusesource.org/rfc/NPL/BAPI_FLCONN_GETDETAIL");
        Resource res = new XMLResourceImpl();
        res.getContents().add(ePackage);
        res.save(System.out, null);
	}
	
	//@Test
	public void testRequest() throws Exception {
		JCoDestination jcoDestination = JCoDestinationManager.getDestination("TestDestination");
		Structure request = RfcUtil.getRequest(jcoDestination.getRepository(), "BAPI_FLCONN_GETDETAIL");
		request.put("TRAVELAGENCYNUMBER", "00000110");
		request.put("CONNECTIONNUMBER", "0002");
		request.put("FLIGHTDATE", new GregorianCalendar(2012, 01, 01).getTime());
		
        Resource res = new XMLResourceImpl();
        res.getContents().add(request);
        res.save(System.out, null);
	}
	
	@Test
	public void testFlightConnectionGetListRequest() throws Exception {

		JCoDestination jcoDestination = JCoDestinationManager.getDestination("TestDestination");
		Structure request = RfcUtil.getRequest(jcoDestination.getRepository(), "BAPI_FLCONN_GETLIST");
		request.put("TRAVELAGENCY", "00000110");

		@SuppressWarnings("unchecked")
		Table<Structure> table = (Table<Structure>) request.get("DATE_RANGE");
		Structure date_range = table.add();
		date_range.put("SIGN", "I");
		date_range.put("OPTION", "EQ");
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		Date flightDate = dateFormat.parse("2012-02-01T00:00:00.000-0500");
		date_range.put("LOW", flightDate);
		
		Structure destination_from = (Structure) request.get("DESTINATION_FROM");
		destination_from.put("AIRPORTID", "SFO");
		
		Structure destination_to = (Structure) request.get("DESTINATION_TO");
		destination_to.put("AIRPORTID", "FRA");
		
		String requestString = RfcUtil.marshal(request);
		
		System.out.println(requestString);
	}

}
