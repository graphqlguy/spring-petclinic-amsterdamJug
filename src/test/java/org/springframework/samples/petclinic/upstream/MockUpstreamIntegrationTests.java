package org.springframework.samples.petclinic.upstream;

import org.junit.jupiter.api.Test;
import org.springframework.graphql.execution.ErrorType;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.havingExactly;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingXPath;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Queries the public services through GraphQL, with the answers recorded in
 * <code>src/main/resources/mock-upstreams</code>.
 */
class MockUpstreamIntegrationTests extends MockUpstreamTests {

	@Test
	void petFoodAlertFromRasff() {
		this.graphQl.document("""
				{
				  rasffNotifications(filter: { productCategoryIds: ["18429"] }, size: 1) {
				    totalNotifications
				    notifications { id reference subject }
				  }
				  rasffNotification(id: "876211") {
				    validationDate
				    product { name category { description } hazards { name category } }
				    risk { decision }
				    countries { code roles }
				  }
				}""")
			.execute()
			.path("rasffNotifications.totalNotifications")
			.entity(Integer.class)
			.isEqualTo(270)
			.path("rasffNotifications.notifications[0].subject")
			.entity(String.class)
			.isEqualTo("Foreign body in pet food for cats from Germany")
			.path("rasffNotification.validationDate")
			.entity(String.class)
			.isEqualTo("2026-09-29")
			.path("rasffNotification.product.category.description")
			.entity(String.class)
			.isEqualTo("pet food")
			.path("rasffNotification.product.hazards[0].name")
			.entity(String.class)
			.isEqualTo("foreign body - foreign bodies")
			.path("rasffNotification.risk.decision")
			.entity(String.class)
			.isEqualTo("potential risk")
			.path("rasffNotification.countries[0].roles")
			.entityList(String.class)
			.containsExactly("FOR_FOLLOW_UP", "ORIGIN", "OPERATOR");
	}

	@Test
	void rasffAnswersAnUnknownNotificationWithUnauthorized() {
		this.graphQl.document("{ rasffNotification(id: \"1\") { reference } }")
			.execute()
			.path("rasffNotification")
			.valueIsNull();
	}

	@Test
	void dogOccurrencesInBelgiumFromGbif() {
		this.mockUpstreams.resetRequests();

		this.graphQl.document("""
				{
				  gbifSearchOccurrences(taxonKey: [6164210], country: [BE], page: 1, size: 2) {
				    count
				    results { key scientificName countryCode eventDate }
				  }
				}""")
			.execute()
			.path("gbifSearchOccurrences.count")
			.entity(Long.class)
			.isEqualTo(3749L)
			.path("gbifSearchOccurrences.results[0].scientificName")
			.entity(String.class)
			.isEqualTo("Canis familiaris Linnaeus, 1758")
			.path("gbifSearchOccurrences.results[0].countryCode")
			.entity(String.class)
			.isEqualTo("BE");

		// pages count from 0, so page 1 with 2 on a page starts at GBIF's offset 2
		this.mockUpstreams.verify(getRequestedFor(urlPathEqualTo("/gbif/v1/occurrence/search"))
			.withQueryParam("taxonKey", equalTo("6164210"))
			.withQueryParam("country", equalTo("BE"))
			.withQueryParam("offset", equalTo("2"))
			.withQueryParam("limit", equalTo("2")));
	}

	@Test
	void dogInTheGbifBackboneWithCountsByKingdom() {
		this.graphQl.document("""
				{
				  gbifNameUsage(usageKey: 6164210) { scientificName rank family }
				  gbifDatasetMetrics(key: "d7dddbf4-2cf0-4f39-9b2a-bb099caae36c") { countByKingdom { key value } }
				}""")
			.execute()
			.path("gbifNameUsage.scientificName")
			.entity(String.class)
			.isEqualTo("Canis lupus familiaris Linnaeus, 1758")
			.path("gbifNameUsage.rank")
			.entity(String.class)
			.isEqualTo("SUBSPECIES")
			// GBIF answers with a map of counts, and the schema has a list of entries
			.path("gbifDatasetMetrics.countByKingdom[0].key")
			.entity(String.class)
			.isEqualTo("ANIMALIA")
			.path("gbifDatasetMetrics.countByKingdom[0].value")
			.entity(Integer.class)
			.isEqualTo(2909803);
	}

	@Test
	void gbifAnswerWithoutContentBecomesNull() {
		this.graphQl.document("""
				{ gbifInstitutionMasterSourceMetadata(key: "6f7571ff-9943-4dee-9707-48743806493e") { source } }""")
			.execute()
			.path("gbifInstitutionMasterSourceMetadata")
			.valueIsNull();
	}

	@Test
	void veterinaryPricesFromTheCbsHicpTable() {
		this.mockUpstreams.resetRequests();
		String filter = "ExpenditureCategories eq 'CPI094500' and substringof('2026MM',Periods)";

		this.graphQl.document("""
				query($filter: String) {
				  cbs86144ENGTypedDataSet(filter: $filter) {
				    ID ExpenditureCategories Periods HICP_1 AnnualRateOfChangeHICP_5 WeightingCoefficient_7
				  }
				}""")
			.variable("filter", filter)
			.execute()
			.path("cbs86144ENGTypedDataSet[*].Periods")
			.entityList(String.class)
			.hasSize(10)
			.path("cbs86144ENGTypedDataSet[9].Periods")
			.entity(String.class)
			.isEqualTo("2026MM10")
			.path("cbs86144ENGTypedDataSet[9].HICP_1")
			.entity(Double.class)
			.isEqualTo(107.32)
			.path("cbs86144ENGTypedDataSet[9].AnnualRateOfChangeHICP_5")
			.entity(Double.class)
			.isEqualTo(5.5)
			.path("cbs86144ENGTypedDataSet[9].WeightingCoefficient_7")
			.entity(Integer.class)
			.isEqualTo(333);

		// the arguments go to CBS as OData query options, and top has a default of 100
		this.mockUpstreams.verify(getRequestedFor(urlPathEqualTo("/cbs/ODataFeed/odata/86144ENG/TypedDataSet"))
			.withQueryParam("$filter", equalTo(filter))
			.withQueryParam("$top", equalTo("100"))
			.withoutQueryParam("$skip"));
	}

	@Test
	void cbsTypedDataSetPassesSkipAndTop() {
		this.mockUpstreams.resetRequests();

		this.graphQl.document("{ cbs84952ENGTypedDataSet(top: 5, skip: 10) { FarmAnimals Periods Livestock_1 } }")
			.execute()
			.path("cbs84952ENGTypedDataSet[0].FarmAnimals")
			.entity(String.class)
			.isEqualTo("A028972")
			.path("cbs84952ENGTypedDataSet[0].Livestock_1")
			.entity(Integer.class)
			.isEqualTo(3551);

		this.mockUpstreams.verify(getRequestedFor(urlPathEqualTo("/cbs/ODataFeed/odata/84952ENG/TypedDataSet"))
			.withQueryParam("$top", equalTo("5"))
			.withQueryParam("$skip", equalTo("10"))
			.withoutQueryParam("$filter"));
	}

	@Test
	void veterinaryPricesFromTheCbsCpiTable() {
		this.graphQl.document("{ cbs86141ENGTypedDataSet { Periods CPI_1 AnnualRateOfChangeCPI_5 } }")
			.execute()
			.path("cbs86141ENGTypedDataSet[9].Periods")
			.entity(String.class)
			.isEqualTo("2026MM10")
			.path("cbs86141ENGTypedDataSet[9].CPI_1")
			.entity(Double.class)
			.isEqualTo(107.32);
	}

	@Test
	void cbsTablesFromTheCatalogue() {
		this.mockUpstreams.resetRequests();
		String filter = "Language eq 'en' and substringof('HICP',Title)";

		this.graphQl.document("""
				query($filter: String) {
				  cbsTables(filter: $filter, top: 5) { Identifier Title Frequency Language RecordCount }
				}""")
			.variable("filter", filter)
			.execute()
			.path("cbsTables[*].Identifier")
			.entityList(String.class)
			.containsExactly("86144ENG", "86145ENG", "86146ENG")
			.path("cbsTables[0].Title")
			.entity(String.class)
			.isEqualTo("Consumer prices; HICP 2025=100, index and rates of change");

		this.mockUpstreams.verify(
				getRequestedFor(urlPathEqualTo("/cbs/ODataCatalog/Tables")).withQueryParam("$filter", equalTo(filter))
					.withQueryParam("$top", equalTo("5")));
	}

	@Test
	void cbsTableInfosOfOneTable() {
		this.graphQl.document("{ cbsTableInfos(table: \"84952ENG\") { Identifier Title Frequency Period } }")
			.execute()
			.path("cbsTableInfos[0].Identifier")
			.entity(String.class)
			.isEqualTo("84952ENG")
			.path("cbsTableInfos[0].Title")
			.entity(String.class)
			.isEqualTo("Livestock on agricultural holdings: reference date 1 April and 1 December")
			.path("cbsTableInfos[0].Frequency")
			.entity(String.class)
			.isEqualTo("Twiceyearly");
	}

	@Test
	void cbsDataPropertiesNameTheColumnsOfATable() {
		this.graphQl.document("{ cbsDataProperties(table: \"86144ENG\") { Type Key Title Unit Datatype } }")
			.execute()
			.path("cbsDataProperties[*].Key")
			.entityList(String.class)
			.containsExactly("ExpenditureCategories", "Periods", "HICP_1", "HICPConstantTaxes_2",
					"MonthlyRateOfChangeHICP_3", "MonthlyRateOfChangeHICPCT_4", "AnnualRateOfChangeHICP_5",
					"AnnualRateOfChangeHICPCT_6", "WeightingCoefficient_7")
			.path("cbsDataProperties[2].Unit")
			.entity(String.class)
			.isEqualTo("2025=100");
	}

	@Test
	void cbsCategoryGroupsOfOneTable() {
		this.graphQl.document("{ cbsCategoryGroups(table: \"86144ENG\") { ID DimensionKey Title ParentID } }")
			.execute()
			.path("cbsCategoryGroups[1].DimensionKey")
			.entity(String.class)
			.isEqualTo("ExpenditureCategories")
			.path("cbsCategoryGroups[1].Title")
			.entity(String.class)
			.isEqualTo("All items");
	}

	@Test
	void cbsPeriodsKeepTheStatusAsCbsWritesIt() {
		this.graphQl.document("{ cbsPeriods(table: \"86144ENG\") { Key Title Status } }")
			.execute()
			.path("cbsPeriods[0].Key")
			.entity(String.class)
			.isEqualTo("2026MM01")
			.path("cbsPeriods[0].Title")
			.entity(String.class)
			.isEqualTo("2026 January")
			// CBS writes the status in Dutch in its English tables too
			.path("cbsPeriods[0].Status")
			.entity(String.class)
			.isEqualTo("Definitief");
	}

	@Test
	void petCategoriesOfTheCbsPriceTables() {
		this.graphQl.document("""
				{
				  cbs86144ENGExpenditureCategories { Key Title }
				  cbs86141ENGExpenditureCategories { Key Title }
				}""")
			.execute()
			.path("cbs86144ENGExpenditureCategories[*].Key")
			.entityList(String.class)
			.containsExactly("CPI051140", "CPI093000", "CPI093200", "CPI093220", "CPI094500")
			.path("cbs86141ENGExpenditureCategories[4].Title")
			.entity(String.class)
			.isEqualTo("094500 Veterinary services, etc.");
	}

	@Test
	void farmAnimalsOfTheCbsLivestockTable() {
		this.graphQl.document("{ cbs84952ENGFarmAnimals { Key Title CategoryGroupID } }")
			.execute()
			.path("cbs84952ENGFarmAnimals[*].Key")
			.entityList(String.class)
			.hasSize(59)
			.path("cbs84952ENGFarmAnimals[0].Title")
			.entity(String.class)
			.isEqualTo("Cattle (total)");
	}

	@Test
	void cbsTableThatDoesNotExistBecomesNull() {
		this.graphQl.document("{ cbsTableInfos(table: \"99999ENG\") { Title } }")
			.execute()
			.path("cbsTableInfos")
			.valueIsNull();
	}

	@Test
	void wolvesGatheredInTheNetherlandsFromNaturalis() {
		this.mockUpstreams.resetRequests();

		this.graphQl.document("""
				{
				  naturalisSpecimenQuery(querySpec: {
				    conditions: [{
				      field: "identifications.scientificName.genusOrMonomial", operator: EQUALS, value: "Canis",
				      and: [{ field: "gatheringEvent.country", operator: EQUALS, value: "Netherlands" }]
				    }],
				    sortFields: [{ path: "gatheringEvent.dateTimeBegin", sortOrder: DESC }],
				    size: 3
				  }) {
				    totalSize
				    resultSet {
				      item {
				        unitID kindOfUnit
				        gatheringEvent { provinceState dateTimeBegin }
				        identifications { scientificName { fullScientificName } }
				      }
				    }
				  }
				}""")
			.execute()
			.path("naturalisSpecimenQuery.totalSize")
			.entity(Integer.class)
			.isEqualTo(569)
			.path("naturalisSpecimenQuery.resultSet[*].item.unitID")
			.entityList(String.class)
			.containsExactly("RMNH.MAM.63929.b", "RMNH.MAM.63929.a", "RMNH.MAM.64305")
			.path("naturalisSpecimenQuery.resultSet[0].item.gatheringEvent.provinceState")
			.entity(String.class)
			.isEqualTo("Drenthe")
			.path("naturalisSpecimenQuery.resultSet[0].item.identifications[0].scientificName.fullScientificName")
			.entity(String.class)
			.isEqualTo("Canis lupus lupus Linnaeus, 1758");

		// the querySpec goes to Naturalis as the JSON body, with the names and values the
		// query gave
		this.mockUpstreams
			.verify(postRequestedFor(urlPathEqualTo("/naturalis/v2/specimen/query")).withRequestBody(equalToJson(
					"""
							{
							  "conditions": [{
							    "field": "identifications.scientificName.genusOrMonomial", "operator": "EQUALS", "value": "Canis",
							    "and": [{ "field": "gatheringEvent.country", "operator": "EQUALS", "value": "Netherlands" }]
							  }],
							  "sortFields": [{ "path": "gatheringEvent.dateTimeBegin", "sortOrder": "DESC" }],
							  "size": 3
							}""")));
	}

	@Test
	void speciesOfAGenusFromNaturalis() {
		this.graphQl.document("""
				{
				  naturalisTaxonQuery(querySpec: {
				    conditions: [{ field: "defaultClassification.genus", operator: EQUALS, value: "Canis" }]
				  }) {
				    totalSize
				    resultSet {
				      score
				      item {
				        id taxonRank
				        sourceSystem { code }
				        acceptedName { fullScientificName }
				        defaultClassification { family order }
				        vernacularNames { name language preferred }
				      }
				    }
				  }
				}""")
			.execute()
			.path("naturalisTaxonQuery.totalSize")
			.entity(Integer.class)
			.isEqualTo(92)
			.path("naturalisTaxonQuery.resultSet[*].item.acceptedName.fullScientificName")
			.entityList(String.class)
			.containsExactly("Canis aureus (Linnaeus, 1758)", "Canis lupus Linnaeus, 1758",
					"Canis lupus arctos Pocock, 1935")
			.path("naturalisTaxonQuery.resultSet[1].item.vernacularNames[0].name")
			.entity(String.class)
			.isEqualTo("Wolf")
			.path("naturalisTaxonQuery.resultSet[0].item.defaultClassification.family")
			.entity(String.class)
			.isEqualTo("Canidae");
	}

	@Test
	void naturalisQueryWithoutAQuerySpecSendsAnEmptyOne() {
		this.mockUpstreams.resetRequests();

		this.graphQl.document("{ naturalisTaxonQuery { totalSize } }")
			.execute()
			.path("naturalisTaxonQuery.totalSize")
			.entity(Integer.class)
			.isEqualTo(92);

		this.mockUpstreams
			.verify(postRequestedFor(urlPathEqualTo("/naturalis/v2/taxon/query")).withRequestBody(equalToJson("{}")));
	}

	@Test
	void naturalisCountsAnswerANumber() {
		this.graphQl.document("""
				{
				  naturalisTaxonCount(querySpec: {
				    conditions: [{ field: "defaultClassification.genus", operator: EQUALS, value: "Canis" }]
				  })
				  naturalisSpecimenCount
				}""")
			.execute()
			.path("naturalisTaxonCount")
			.entity(Integer.class)
			.isEqualTo(92)
			.path("naturalisSpecimenCount")
			.entity(Integer.class)
			.isEqualTo(569);
	}

	@Test
	void oneTaxonFromNaturalis() {
		this.graphQl.document("""
				{
				  naturalisTaxonFind(id: "9I9KO475EQS@NSR") {
				    recordURI occurrenceStatusVerbatim
				    acceptedName { fullScientificName author year }
				    systemClassification { rank name }
				  }
				}""")
			.execute()
			.path("naturalisTaxonFind.acceptedName.fullScientificName")
			.entity(String.class)
			.isEqualTo("Canis aureus (Linnaeus, 1758)")
			.path("naturalisTaxonFind.systemClassification[4].name")
			.entity(String.class)
			.isEqualTo("Canidae");
	}

	@Test
	void naturalisTaxonThatDoesNotExistBecomesNull() {
		this.graphQl.document("{ naturalisTaxonFind(id: \"nope\") { id } }")
			.execute()
			.path("naturalisTaxonFind")
			.valueIsNull();
	}

	@Test
	void oneSpecimenFromNaturalis() {
		this.graphQl.document("""
				{
				  naturalisSpecimenFind(id: "RMNH.MAM.63929.b@CRS") { unitID recordBasis sex license numberOfSpecimen }
				  naturalisSpecimenFindByUnitID(unitID: "RMNH.MAM.63929.b") { id collectionType preparationType }
				}""")
			.execute()
			.path("naturalisSpecimenFind.recordBasis")
			.entity(String.class)
			.isEqualTo("PreservedSpecimen")
			.path("naturalisSpecimenFind.sex")
			.entity(String.class)
			.isEqualTo("female")
			.path("naturalisSpecimenFind.numberOfSpecimen")
			.entity(Integer.class)
			.isEqualTo(1)
			.path("naturalisSpecimenFindByUnitID[0].id")
			.entity(String.class)
			.isEqualTo("RMNH.MAM.63929.b@CRS")
			.path("naturalisSpecimenFindByUnitID[0].preparationType")
			.entity(String.class)
			.isEqualTo("mounted skin");
	}

	@Test
	void naturalisSaysWhetherASpecimenExists() {
		this.graphQl.document("""
				{
				  known: naturalisSpecimenExists(unitID: "RMNH.MAM.63929.b")
				  unknown: naturalisSpecimenExists(unitID: "nope")
				}""")
			.execute()
			.path("known")
			.entity(Boolean.class)
			.isEqualTo(true)
			.path("unknown")
			.entity(Boolean.class)
			.isEqualTo(false);
	}

	@Test
	void naturalisRejectsAConditionOnAFieldItDoesNotHave() {
		this.graphQl.document("""
				{
				  naturalisSpecimenQuery(querySpec: { conditions: [{ field: "nope", operator: EQUALS, value: "x" }] }) {
				    totalSize
				  }
				}""").execute().errors().satisfy((errors) -> assertThat(errors).singleElement().satisfies((error) -> {
			assertThat(error.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
			assertThat(error.getMessage()).contains("Invalid element \"nope\" in path \"nope\"");
		}));
	}

	@Test
	void nitrogenDioxideAtAnAmsterdamStationFromLuchtmeetnet() {
		this.mockUpstreams.resetRequests();

		this.graphQl.document("""
				{
				  luchtmeetnetMeasurements(station_number: "NL49012", formula: "NO2", start: "2026-11-14T00:00:00",
				      end: "2026-11-14T06:00:00", order_by: "timestamp_measured", order_direction: "asc", page: 1) {
				    pagination { current_page last_page }
				    data { station_number formula value timestamp_measured timestamp_measured_start }
				  }
				}""")
			.execute()
			.path("luchtmeetnetMeasurements.pagination.current_page")
			.entity(Integer.class)
			.isEqualTo(1)
			.path("luchtmeetnetMeasurements.data[*].value")
			.entityList(Double.class)
			.hasSize(7)
			.path("luchtmeetnetMeasurements.data[0].value")
			.entity(Double.class)
			.isEqualTo(59.0)
			.path("luchtmeetnetMeasurements.data[0].timestamp_measured")
			.entity(String.class)
			.isEqualTo("2026-11-14T00:00:00+00:00");

		// the arguments go to Luchtmeetnet as query parameters of the same name
		this.mockUpstreams.verify(getRequestedFor(urlPathEqualTo("/luchtmeetnet/open_api/measurements"))
			.withQueryParam("station_number", equalTo("NL49012"))
			.withQueryParam("formula", equalTo("NO2"))
			.withQueryParam("start", equalTo("2026-11-14T00:00:00"))
			.withQueryParam("end", equalTo("2026-11-14T06:00:00"))
			.withQueryParam("order_by", equalTo("timestamp_measured"))
			.withQueryParam("order_direction", equalTo("asc"))
			.withQueryParam("page", equalTo("1")));
	}

	@Test
	void luchtmeetnetLeavesOutTheArgumentsAQueryLeavesOut() {
		this.mockUpstreams.resetRequests();

		this.graphQl.document("{ luchtmeetnetStations { data { number location } } }")
			.execute()
			.path("luchtmeetnetStations.data[0].number")
			.entity(String.class)
			.isEqualTo("NL49565");

		this.mockUpstreams
			.verify(getRequestedFor(urlPathEqualTo("/luchtmeetnet/open_api/stations")).withoutQueryParam("page")
				.withoutQueryParam("order_by")
				.withoutQueryParam("organisation_id"));
	}

	@Test
	void oneStationFromLuchtmeetnet() {
		this.graphQl.document("""
				{
				  luchtmeetnetStation(number: "NL49012") {
				    data {
				      location type municipality organisation year_start components
				      description { EN } geometry { type coordinates }
				    }
				  }
				}""")
			.execute()
			.path("luchtmeetnetStation.data.location")
			.entity(String.class)
			.isEqualTo("Amsterdam-Van Diemenstraat")
			.path("luchtmeetnetStation.data.municipality")
			.entity(String.class)
			.isEqualTo("Amsterdam")
			.path("luchtmeetnetStation.data.components")
			.entityList(String.class)
			.contains("NO2", "PM10")
			.path("luchtmeetnetStation.data.geometry.coordinates")
			.entityList(Double.class)
			.containsExactly(4.887811, 52.389983);
	}

	@Test
	void luchtmeetnetStationThatDoesNotExistBecomesNull() {
		this.graphQl.document("{ luchtmeetnetStation(number: \"NOPE\") { data { location } } }")
			.execute()
			.path("luchtmeetnetStation")
			.valueIsNull();
	}

	@Test
	void measurementsOfOneLuchtmeetnetStation() {
		this.mockUpstreams.resetRequests();

		this.graphQl.document("""
				{
				  luchtmeetnetStationMeasurements(number: "NL49012", formula: "NO2", page: 1) {
				    pagination { last_page }
				    data { formula value timestamp_measured }
				  }
				}""")
			.execute()
			.path("luchtmeetnetStationMeasurements.data[0].value")
			.entity(Double.class)
			.isEqualTo(38.3);

		this.mockUpstreams
			.verify(getRequestedFor(urlPathEqualTo("/luchtmeetnet/open_api/stations/NL49012/measurements"))
				.withQueryParam("formula", equalTo("NO2"))
				.withQueryParam("page", equalTo("1")));
	}

	@Test
	void airQualityIndexFromLuchtmeetnet() {
		this.graphQl
			.document(
					"""
							{
							  luchtmeetnetLki(station_number: "NL49012") { data { station_number formula value timestamp_measured } }
							}""")
			.execute()
			.path("luchtmeetnetLki.data[0].formula")
			.entity(String.class)
			.isEqualTo("LKI")
			.path("luchtmeetnetLki.data[0].value")
			.entity(Double.class)
			.isEqualTo(5.0);
	}

	@Test
	void concentrationsAtALocationFromLuchtmeetnet() {
		this.mockUpstreams.resetRequests();

		this.graphQl.document("""
				{
				  luchtmeetnetConcentrations(formula: "NO2", latitude: 52.37, longitude: 4.89) {
				    data { formula value timestamp_measured }
				  }
				}""")
			.execute()
			.path("luchtmeetnetConcentrations.data[*].formula")
			.entityList(String.class)
			.hasSize(19)
			.path("luchtmeetnetConcentrations.data[0].timestamp_measured")
			.entity(String.class)
			.isEqualTo("2026-11-14T23:00:00+00:00");

		this.mockUpstreams.verify(getRequestedFor(urlPathEqualTo("/luchtmeetnet/open_api/concentrations"))
			.withQueryParam("formula", equalTo("NO2"))
			.withQueryParam("latitude", equalTo("52.37"))
			.withQueryParam("longitude", equalTo("4.89")));
	}

	@Test
	void componentsAndOrganisationsFromLuchtmeetnet() {
		this.graphQl
			.document(
					"""
							{
							  luchtmeetnetComponents { data { formula name { EN NL } } }
							  luchtmeetnetComponent(formula: "NO2") { data { formula name { EN } limits { rating lowerband upperband } } }
							  luchtmeetnetOrganisations { data { id name { EN } } }
							}""")
			.execute()
			.path("luchtmeetnetComponents.data[8].name.EN")
			.entity(String.class)
			.isEqualTo("Nitrogen dioxide (NO2)")
			.path("luchtmeetnetComponent.data.limits[1].lowerband")
			.entity(Double.class)
			.isEqualTo(10.0)
			.path("luchtmeetnetComponent.data.limits[1].upperband")
			.entity(Double.class)
			.isEqualTo(20.0)
			.path("luchtmeetnetOrganisations.data[1].name.EN")
			.entity(String.class)
			.isEqualTo("GGD Amsterdam");
	}

	@Test
	void adverseEventsOfDogsFromOpenFda() {
		this.mockUpstreams.resetRequests();
		String search = "animal.species:\"Dog\" AND original_receive_date:[20260101 TO 20261231]";

		this.graphQl.document("""
				query($search: String) {
				  openfdaAnimalAndVeterinaryEvents(search: $search, sort: "original_receive_date:desc", limit: 3) {
				    meta { last_updated results { skip limit total } }
				    results {
				      unique_aer_id_number original_receive_date serious_ae
				      animal { species gender age { min unit } breed { is_crossbred } }
				      drug { brand_name route active_ingredients { name dose { numerator numerator_unit } } }
				      reaction { veddra_term_name }
				      outcome { medical_status }
				    }
				  }
				}""")
			.variable("search", search)
			.execute()
			.path("openfdaAnimalAndVeterinaryEvents.meta.results.total")
			.entity(Integer.class)
			.isEqualTo(26143)
			.path("openfdaAnimalAndVeterinaryEvents.results[*].unique_aer_id_number")
			.entityList(String.class)
			.containsExactly("USA-USFDACVM-2026-US-035565", "USA-USFDACVM-2026-US-035646",
					"USA-USFDACVM-2026-US-035558")
			.path("openfdaAnimalAndVeterinaryEvents.results[0].animal.species")
			.entity(String.class)
			.isEqualTo("Dog")
			// openFDA sends every value of a report as text, dates as YYYYMMDD
			.path("openfdaAnimalAndVeterinaryEvents.results[0].original_receive_date")
			.entity(String.class)
			.isEqualTo("20260626")
			.path("openfdaAnimalAndVeterinaryEvents.results[2].reaction[0].veddra_term_name")
			.entity(String.class)
			.isEqualTo("Vomiting");

		this.mockUpstreams.verify(getRequestedFor(urlPathEqualTo("/openfda/animalandveterinary/event.json"))
			.withQueryParam("search", equalTo(search))
			.withQueryParam("sort", equalTo("original_receive_date:desc"))
			.withQueryParam("limit", equalTo("3"))
			.withoutQueryParam("skip")
			.withoutQueryParam("count"));
	}

	@Test
	void mostReportedReactionsOfDogsFromOpenFda() {
		this.mockUpstreams.resetRequests();

		this.graphQl.document("""
				{
				  openfdaAnimalAndVeterinaryEventCounts(search: "animal.species:Dog",
				      count: "reaction.veddra_term_name.exact", limit: 5) {
				    results { term count }
				  }
				}""")
			.execute()
			.path("openfdaAnimalAndVeterinaryEventCounts.results[0].term")
			.entity(String.class)
			.isEqualTo("Vomiting")
			.path("openfdaAnimalAndVeterinaryEventCounts.results[0].count")
			.entity(Integer.class)
			.isEqualTo(196274);

		this.mockUpstreams.verify(getRequestedFor(urlPathEqualTo("/openfda/animalandveterinary/event.json"))
			.withQueryParam("search", equalTo("animal.species:Dog"))
			.withQueryParam("count", equalTo("reaction.veddra_term_name.exact"))
			.withQueryParam("limit", equalTo("5")));
	}

	@Test
	void openFdaCountsByDateAnswerATime() {
		this.graphQl.document("""
				{ openfdaAnimalAndVeterinaryEventCounts(count: "original_receive_date") { results { time count } } }""")
			.execute()
			.path("openfdaAnimalAndVeterinaryEventCounts.results[0].time")
			.entity(String.class)
			.isEqualTo("20260601")
			.path("openfdaAnimalAndVeterinaryEventCounts.results[0].count")
			.entity(Integer.class)
			.isEqualTo(53);
	}

	@Test
	void openFdaSearchWithoutMatchesBecomesNull() {
		this.graphQl.document("""
				{ openfdaAnimalAndVeterinaryEvents(search: "animal.species:Zzz") { results { report_id } } }""")
			.execute()
			.path("openfdaAnimalAndVeterinaryEvents")
			.valueIsNull();
	}

	@Test
	void openFdaRejectsALimitAboveOneThousand() {
		this.graphQl.document("{ openfdaAnimalAndVeterinaryEvents(limit: 2000) { results { report_id } } }")
			.execute()
			.errors()
			.satisfy((errors) -> assertThat(errors).singleElement().satisfies((error) -> {
				assertThat(error.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
				assertThat(error.getMessage()).contains("Limit cannot exceed 1000 results for search requests");
			}));
	}

	@Test
	void mammalsSeenInAmsterdamFromInaturalist() {
		this.mockUpstreams.resetRequests();

		this.graphQl
			.document(
					"""
							{
							  inaturalistObservationSpeciesCounts(place_id: "17309", iconic_taxa: "Mammalia", year: "2026", per_page: 5) {
							    total_results page per_page
							    results {
							      count
							      taxon {
							        id name rank preferred_common_name iconic_taxon_name observations_count wikipedia_url
							        preferred_establishment_means
							        establishment_means { establishment_means place { id display_name } }
							      }
							    }
							  }
							}""")
			.execute()
			.path("inaturalistObservationSpeciesCounts.total_results")
			.entity(Integer.class)
			.isEqualTo(59)
			.path("inaturalistObservationSpeciesCounts.results[*].taxon.preferred_common_name")
			.entityList(String.class)
			.containsExactly("Brown Rat", "European Rabbit", "Brown Hare", "Muskrat", "European Mole")
			.path("inaturalistObservationSpeciesCounts.results[0].count")
			.entity(Integer.class)
			.isEqualTo(22)
			.path("inaturalistObservationSpeciesCounts.results[0].taxon.establishment_means.place.display_name")
			.entity(String.class)
			.isEqualTo("Amsterdam, NH, NL");

		// the arguments go to iNaturalist as query parameters of the same name
		this.mockUpstreams.verify(getRequestedFor(urlPathEqualTo("/inaturalist/v1/observations/species_counts"))
			.withQueryParam("place_id", equalTo("17309"))
			.withQueryParam("iconic_taxa", equalTo("Mammalia"))
			.withQueryParam("year", equalTo("2026"))
			.withQueryParam("per_page", equalTo("5"))
			.withoutQueryParam("taxon_id")
			.withoutQueryParam("d1"));
	}

	@Test
	void amsterdamAsAPlaceFromInaturalist() {
		this.mockUpstreams.resetRequests();

		this.graphQl.document("""
				{
				  inaturalistPlaceAutocomplete(q: "Amsterdam, NH", per_page: 3) {
				    total_results
				    results { id name display_name admin_level location ancestor_place_ids }
				  }
				  inaturalistPlaceDetails(id: "17309") { results { id slug display_name } }
				}""")
			.execute()
			.path("inaturalistPlaceAutocomplete.results[0].id")
			.entity(Integer.class)
			.isEqualTo(17309)
			.path("inaturalistPlaceAutocomplete.results[0].display_name")
			.entity(String.class)
			.isEqualTo("Amsterdam, NH, NL")
			.path("inaturalistPlaceDetails.results[0].slug")
			.entity(String.class)
			.isEqualTo("amsterdam-noord-holland-nl");

		this.mockUpstreams.verify(getRequestedFor(urlPathEqualTo("/inaturalist/v1/places/autocomplete"))
			.withQueryParam("q", equalTo("Amsterdam, NH"))
			.withQueryParam("per_page", equalTo("3")));
		this.mockUpstreams.verify(getRequestedFor(urlPathEqualTo("/inaturalist/v1/places/17309")));
	}

	@Test
	void taxaByNameFromInaturalist() {
		this.mockUpstreams.resetRequests();

		this.graphQl.document("""
				{
				  inaturalistTaxonSearch(q: "dog", rank: "species", per_page: 3) {
				    total_results
				    results { id name rank rank_level preferred_common_name matched_term is_active extinct }
				  }
				  inaturalistTaxonAutocomplete(q: "hedgehog", per_page: 3) { results { id name preferred_common_name } }
				}""")
			.execute()
			.path("inaturalistTaxonSearch.total_results")
			.entity(Integer.class)
			.isEqualTo(689)
			.path("inaturalistTaxonSearch.results[2].name")
			.entity(String.class)
			.isEqualTo("Canis familiaris")
			.path("inaturalistTaxonSearch.results[2].preferred_common_name")
			.entity(String.class)
			.isEqualTo("Domestic Dog")
			.path("inaturalistTaxonAutocomplete.results[0].name")
			.entity(String.class)
			.isEqualTo("Erinaceus europaeus");

		this.mockUpstreams
			.verify(getRequestedFor(urlPathEqualTo("/inaturalist/v1/taxa")).withQueryParam("q", equalTo("dog"))
				.withQueryParam("rank", equalTo("species"))
				.withQueryParam("per_page", equalTo("3")));
	}

	@Test
	void oneTaxonWithItsAncestorsFromInaturalist() {
		this.graphQl.document("""
				{
				  inaturalistTaxonDetails(id: "47144") {
				    results {
				      id name preferred_common_name wikipedia_summary
				      default_photo { url medium_url license_code attribution }
				      ancestors { rank name }
				      children { name preferred_common_name }
				    }
				  }
				}""")
			.execute()
			.path("inaturalistTaxonDetails.results[0].name")
			.entity(String.class)
			.isEqualTo("Canis familiaris")
			.path("inaturalistTaxonDetails.results[0].ancestors[*].name")
			.entityList(String.class)
			.contains("Carnivora", "Canidae", "Canis")
			.path("inaturalistTaxonDetails.results[0].children[*].name")
			.entityList(String.class)
			.hasSize(2);
	}

	@Test
	void reportsOfLooseDogsOnTheMapOfAmsterdam() {
		this.mockUpstreams.resetRequests();

		this.graphQl.document("""
				{
				  amsterdamSignalsGeography(bbox: "4.72,52.27,5.08,52.44",
				      maincategory_slug: ["overlast-van-en-door-personen-of-groepen"],
				      category_slug: ["loslopende-agressieve-honden"]) {
				    type
				    features {
				      type
				      geometry { type coordinates }
				      properties { created_at category { name slug parent { name slug } } }
				    }
				  }
				}""")
			.execute()
			.path("amsterdamSignalsGeography.type")
			.entity(String.class)
			.isEqualTo("FeatureCollection")
			.path("amsterdamSignalsGeography.features[*].type")
			.entityList(String.class)
			.hasSize(15)
			// the city writes its category names in Dutch
			.path("amsterdamSignalsGeography.features[0].properties.category.name")
			.entity(String.class)
			.isEqualTo("Loslopende of agressieve honden")
			.path("amsterdamSignalsGeography.features[0].properties.created_at")
			.entity(String.class)
			.isEqualTo("2026-11-10T10:36:21.515178+00:00")
			.path("amsterdamSignalsGeography.features[0].geometry.coordinates")
			.entityList(Double.class)
			.containsExactly(5.042705286, 52.304686514);

		this.mockUpstreams
			.verify(getRequestedFor(urlPathEqualTo("/amsterdam-signals/signals/v1/public/signals/geography/"))
				.withQueryParam("bbox", equalTo("4.72,52.27,5.08,52.44"))
				.withQueryParam("maincategory_slug", equalTo("overlast-van-en-door-personen-of-groepen"))
				.withQueryParam("category_slug", equalTo("loslopende-agressieve-honden"))
				.withoutQueryParam("lat")
				.withoutQueryParam("ordering"));
	}

	@Test
	void amsterdamSignalsSendsEachCategoryAsItsOwnParameter() {
		this.mockUpstreams.resetRequests();

		this.graphQl.document("""
				{
				  amsterdamSignalsGeography(bbox: "4.72,52.27,5.08,52.44", category_slug: ["ratten", "wespen"]) { type }
				}""")
			.execute()
			.path("amsterdamSignalsGeography.type")
			.entity(String.class)
			.isEqualTo("FeatureCollection");

		this.mockUpstreams
			.verify(getRequestedFor(urlPathEqualTo("/amsterdam-signals/signals/v1/public/signals/geography/"))
				.withQueryParam("category_slug", havingExactly("ratten", "wespen")));
	}

	@Test
	void amsterdamSignalsRejectsACategoryItDoesNotHave() {
		this.graphQl.document(
				"{ amsterdamSignalsGeography(bbox: \"4.72,52.27,5.08,52.44\", category_slug: [\"nope\"]) { type } }")
			.execute()
			.errors()
			.satisfy((errors) -> assertThat(errors).singleElement().satisfies((error) -> {
				assertThat(error.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
				assertThat(error.getMessage()).contains("nope is geen beschikbare keuze");
			}));
	}

	@Test
	void categoriesOfAmsterdamSignals() {
		this.graphQl.document("""
				{
				  amsterdamSignalsCategories(page_size: 3) { count results { name slug is_public_accessible } }
				  amsterdamSignalsCategory(slug: "overlast-van-dieren") {
				    _display name slug
				    sub_categories { name slug is_public_accessible }
				  }
				  amsterdamSignalsSubCategories(maincategory_slug: "overlast-van-dieren") { count results { slug } }
				}""")
			.execute()
			.path("amsterdamSignalsCategory.name")
			.entity(String.class)
			.isEqualTo("Overlast van dieren")
			.path("amsterdamSignalsCategory.sub_categories[*].slug")
			.entityList(String.class)
			.contains("ratten", "wespen", "dode-dieren")
			.path("amsterdamSignalsSubCategories.count")
			.entity(Integer.class)
			.isEqualTo(8)
			.path("amsterdamSignalsCategories.results[*].slug")
			.entityList(String.class)
			.hasSize(3);
	}

	@Test
	void oneSubCategoryOfAmsterdamSignals() {
		this.graphQl.document("""
				{
				  amsterdamSignalsSubCategory(maincategory_slug: "overlast-van-en-door-personen-of-groepen",
				      slug: "loslopende-agressieve-honden") {
				    name public_name slug description handling_message is_active is_public_accessible
				    departments { code name }
				  }
				}""")
			.execute()
			// the city has an internal name and a name for the public
			.path("amsterdamSignalsSubCategory.name")
			.entity(String.class)
			.isEqualTo("Loslopende/agressieve honden")
			.path("amsterdamSignalsSubCategory.public_name")
			.entity(String.class)
			.isEqualTo("Loslopende of agressieve honden")
			.path("amsterdamSignalsSubCategory.is_active")
			.entity(Boolean.class)
			.isEqualTo(true)
			.path("amsterdamSignalsSubCategory.departments[*].code")
			.entityList(String.class)
			.hasSizeGreaterThan(0);
	}

	@Test
	void amsterdamSignalsCategoryThatDoesNotExistBecomesNull() {
		this.graphQl.document("{ amsterdamSignalsCategory(slug: \"nope\") { name } }")
			.execute()
			.path("amsterdamSignalsCategory")
			.valueIsNull();
	}

	@Test
	void districtsOfAmsterdamFromItsSignalsApi() {
		this.mockUpstreams.resetRequests();

		this.graphQl.document("""
				{
				  amsterdamSignalsAreas(type_code: ["stadsdeel"], page_size: 3) {
				    count results { name code bbox type { id name code } }
				  }
				}""")
			.execute()
			.path("amsterdamSignalsAreas.count")
			.entity(Integer.class)
			.isEqualTo(9)
			.path("amsterdamSignalsAreas.results[0].name")
			.entity(String.class)
			.isEqualTo("Centrum")
			.path("amsterdamSignalsAreas.results[0].bbox")
			.entityList(Double.class)
			.hasSize(4);

		this.mockUpstreams.verify(getRequestedFor(urlPathEqualTo("/amsterdam-signals/signals/v1/public/areas/"))
			.withQueryParam("type_code", equalTo("stadsdeel"))
			.withQueryParam("page_size", equalTo("3")));
	}

	@Test
	void taurineInTheEuRegisterOfFeedAdditives() {
		this.mockUpstreams.resetRequests();

		this.graphQl.document("""
				{
				  euFeedAdditivesDetails(additive_code: "3a370") {
				    nextLink
				    value {
				      policy_item_code additive_name additive_code cat_funct_grp animal_species to_be_phased_out
				      auth_date auth_expiry_date legislation_purpose legislation_reference legislation_url fip_url
				    }
				  }
				}""")
			.execute()
			.path("euFeedAdditivesDetails.value[*].additive_name")
			.entityList(String.class)
			.containsExactly("Taurine", "Taurine", "Taurine")
			.path("euFeedAdditivesDetails.value[0].animal_species")
			.entity(String.class)
			.satisfies((species) -> assertThat(species).startsWith("Canidae - Felidae - Mustelidae"))
			.path("euFeedAdditivesDetails.value[0].auth_expiry_date")
			.entity(String.class)
			.isEqualTo("2036-08-19T12:00:00")
			.path("euFeedAdditivesDetails.nextLink")
			.valueIsNull();

		// format and api-version are fixed, and the arguments keep their names
		this.mockUpstreams
			.verify(getRequestedFor(urlPathEqualTo("/eu-feed-additives/feed-additives/feed-additives-details"))
				.withQueryParam("additive_code", equalTo("3a370"))
				.withQueryParam("format", equalTo("json"))
				.withQueryParam("api-version", equalTo("v2.0"))
				.withoutQueryParam("policy_item_code")
				.withoutQueryParam("$after"));
	}

	@Test
	void oneAdditiveInTheEuList() {
		this.graphQl.document("""
				{
				  euFeedAdditivesList(additive_code: "3a370") {
				    value { policy_item_id policy_item_code display_name additive_code subclass to_be_phased_out }
				  }
				}""")
			.execute()
			.path("euFeedAdditivesList.value[0].display_name")
			.entity(String.class)
			.isEqualTo("Taurine")
			.path("euFeedAdditivesList.value[0].policy_item_code")
			.entity(String.class)
			.isEqualTo("POL-FEED-IMPORT-1225");
	}

	@Test
	void euFeedAdditiveAuthorisationsBeingPhasedOut() {
		this.mockUpstreams.resetRequests();

		this.graphQl.document("""
				{
				  euFeedAdditivesAuthorizations(to_be_phased_out: "Yes", after: "abc") {
				    nextLink
				    value { additive_code additive_name animal_species to_be_phased_out }
				  }
				}""")
			.execute()
			.path("euFeedAdditivesAuthorizations.value[*].to_be_phased_out")
			.entityList(String.class)
			.hasSize(100)
			.path("euFeedAdditivesAuthorizations.nextLink")
			.entity(String.class)
			.satisfies((link) -> assertThat(link).contains("$after="));

		// the token of the next page goes out as $after
		this.mockUpstreams
			.verify(getRequestedFor(urlPathEqualTo("/eu-feed-additives/feed-additives/feed-additives-authorizations"))
				.withQueryParam("to_be_phased_out", equalTo("Yes"))
				.withQueryParam("$after", equalTo("abc"))
				.withoutQueryParam("cat_funct_grp"));
	}

	@Test
	void veterinaryPapersFromEuropePmcOverSoap() {
		this.mockUpstreams.resetRequests();

		this.graphQl
			.document(
					"""
							{
							  europePmcSearchPublications(queryString: "canine parvovirus vaccine", resultType: "lite", pageSize: 3) {
							    version hitCount nextCursorMark
							    request { queryString resultType cursorMark pageSize }
							    resultList {
							      result {
							        id source pmid pmcid doi title authorString journalTitle pubYear isOpenAccess citedByCount
							        firstPublicationDate fullTextIdList { fullTextId }
							      }
							    }
							  }
							}""")
			.execute()
			.path("europePmcSearchPublications.hitCount")
			.entity(Integer.class)
			.isEqualTo(2734)
			.path("europePmcSearchPublications.request.resultType")
			.entity(String.class)
			.isEqualTo("LITE")
			.path("europePmcSearchPublications.resultList.result[*].id")
			.entityList(String.class)
			.hasSize(3)
			.path("europePmcSearchPublications.resultList.result[1].title")
			.entity(String.class)
			.satisfies((title) -> assertThat(title).startsWith("Development of a Novel Canine Parvovirus Vaccine"))
			.path("europePmcSearchPublications.resultList.result[0].journalTitle")
			.entity(String.class)
			.isEqualTo("Vaccines (Basel)")
			.path("europePmcSearchPublications.resultList.result[0].citedByCount")
			.entity(Integer.class)
			.isEqualTo(2)
			.path("europePmcSearchPublications.resultList.result[0].fullTextIdList.fullTextId")
			.entityList(String.class)
			.containsExactly("PMC12390283");

		// the arguments go out as the elements of the SOAP request, under their own names
		this.mockUpstreams.verify(postRequestedFor(urlPathEqualTo("/europepmc/europepmc/webservices/soap"))
			.withRequestBody(matchingXPath("//*[local-name()='searchPublications']"
					+ "[queryString='canine parvovirus vaccine'][resultType='lite'][cursorMark='*'][pageSize='3']"
					+ "[not(sort)][not(synonym)]")));
	}

	@Test
	void papersThatCiteAPaperFromEuropePmcOverSoap() {
		this.graphQl.document("""
				{
				  europePmcGetCitations(id: "37766175", source: "MED", offSet: 0, pageSize: 3) {
				    hitCount
				    request { id source offSet pageSize }
				    citationList { citation { id source citationType title authorString journalAbbreviation pubYear } }
				  }
				}""")
			.execute()
			.path("europePmcGetCitations.hitCount")
			.entity(Integer.class)
			.isEqualTo(14)
			.path("europePmcGetCitations.citationList.citation[*].id")
			.entityList(String.class)
			.hasSize(3)
			.path("europePmcGetCitations.citationList.citation[0].journalAbbreviation")
			.entity(String.class)
			.isEqualTo("Vet Anim Sci");
	}

	@Test
	void europePmcPaperWithoutCitationsHasAnEmptyList() {
		this.graphQl.document("""
				{ europePmcGetCitations(id: "0", source: "MED") { hitCount citationList { citation { id } } } }""")
			.execute()
			.path("europePmcGetCitations.hitCount")
			.entity(Integer.class)
			.isEqualTo(0)
			.path("europePmcGetCitations.citationList.citation")
			.valueIsNull();
	}

	@Test
	void referencesOfAPaperFromEuropePmcOverSoap() {
		this.mockUpstreams.resetRequests();

		this.graphQl.document("""
				{
				  europePmcGetReferences(id: "37766175", source: "MED", pageSize: 3) {
				    hitCount
				    referenceList { reference { id title journalAbbreviation pubYear ISSN citedOrder match } }
				  }
				}""")
			.execute()
			.path("europePmcGetReferences.hitCount")
			.entity(Integer.class)
			.isEqualTo(26)
			.path("europePmcGetReferences.referenceList.reference[0].citedOrder")
			.entity(Integer.class)
			.isEqualTo(1)
			.path("europePmcGetReferences.referenceList.reference[0].journalAbbreviation")
			.entity(String.class)
			.isEqualTo("Vet Microbiol");

		this.mockUpstreams.verify(
				postRequestedFor(urlPathEqualTo("/europepmc/europepmc/webservices/soap")).withRequestBody(matchingXPath(
						"//*[local-name()='getReferences'][id='37766175'][source='MED'][pageSize='3'][not(offSet)]")));
	}

	@Test
	void europePmcSoapFaultReachesTheClient() {
		this.graphQl.document("{ europePmcSearchPublications(queryString: \"\") { hitCount } }")
			.execute()
			.errors()
			.satisfy((errors) -> assertThat(errors).singleElement().satisfies((error) -> {
				assertThat(error.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
				assertThat(error.getMessage()).contains("No search criteria provided");
			}));
	}

}
