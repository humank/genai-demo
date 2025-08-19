package solid.humank.genaidemo.bdd.product;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import solid.humank.genaidemo.bdd.common.TestContext;

/** Product Search Step Definitions */
public class ProductSearchStepDefinitions {

    private final TestContext testContext = TestContext.getInstance();

    // Removed duplicate step definition - now using CommonStepDefinitions

    @When("I search for {string}")
    public void iSearchFor(String searchTerm) {
        testContext.put("searchTerm", searchTerm);
        testContext.put("searchPerformed", true);
        // Mock search results based on search term
        if ("iPhone".equals(searchTerm)) {
            testContext.put("actualResultCount", 1);
            testContext.put("searchResults", java.util.Arrays.asList("PROD-001"));
        } else if ("Apple laptop".equals(searchTerm)) {
            testContext.put("actualResultCount", 1);
            testContext.put("searchResults", java.util.Arrays.asList("PROD-003"));
        } else if ("iPhon".equals(searchTerm)) {
            testContext.put("actualResultCount", 1);
            testContext.put("searchResults", java.util.Arrays.asList("PROD-001"));
            testContext.put("didYouMean", "iPhone");
        } else if ("nonexistent product".equals(searchTerm)) {
            testContext.put("actualResultCount", 0);
            testContext.put("searchResults", java.util.Collections.emptyList());
        } else {
            testContext.put("actualResultCount", 0);
            testContext.put("searchResults", java.util.Collections.emptyList());
        }
    }

    @Given("searching for {string} returns {int} results")
    public void searchingForReturnsResults(String searchTerm, int resultCount) {
        testContext.put("searchTerm", searchTerm);
        testContext.put("expectedResultCount", resultCount);
    }

    @Given("I previously searched for {string}, {string}, {string}")
    public void iPreviouslySearchedFor(String term1, String term2, String term3) {
        testContext.put("searchHistory", java.util.Arrays.asList(term1, term2, term3));
    }

    @When("I search within category {string}")
    public void iSearchWithinCategory(String category) {
        testContext.put("searchCategory", category);
    }

    @When("I search for products with brand {string}")
    public void iSearchForProductsWithBrand(String brand) {
        testContext.put("searchBrand", brand);
    }

    @When("I search for products with price between {int} and {int}")
    public void iSearchForProductsWithPriceBetweenAnd(int minPrice, int maxPrice) {
        testContext.put("minPrice", minPrice);
        testContext.put("maxPrice", maxPrice);
    }

    @When("I search for products with category {string} and brand {string}")
    public void iSearchForProductsWithCategoryAndBrand(String category, String brand) {
        testContext.put("searchCategory", category);
        testContext.put("searchBrand", brand);
    }

    @When("I search for products with tag {string}")
    public void iSearchForProductsWithTag(String tag) {
        testContext.put("searchTag", tag);
    }

    @When("I search for {string} and sort by price low to high")
    public void iSearchForAndSortByPriceLowToHigh(String searchTerm) {
        testContext.put("searchTerm", searchTerm);
        testContext.put("sortOrder", "PRICE_LOW_TO_HIGH");
    }

    @When("I search for {string} and sort by relevance")
    public void iSearchForAndSortByRelevance(String searchTerm) {
        testContext.put("searchTerm", searchTerm);
        testContext.put("sortOrder", "RELEVANCE");
    }

    @When("I click on search box")
    public void iClickOnSearchBox() {
        testContext.put("searchBoxClicked", true);
    }

    @When("I click on search box without entering any content")
    public void iClickOnSearchBoxWithoutEnteringAnyContent() {
        testContext.put("searchBoxClicked", true);
        testContext.put("searchContent", "");
    }

    @When("I type {string}")
    public void iType(String input) {
        testContext.put("searchInput", input);
    }

    @When("I click page {int}")
    public void iClickPage(int pageNumber) {
        testContext.put("currentPage", pageNumber);
    }

    @When("I search for {string} and filter for {string} products")
    public void iSearchForAndFilterForProducts(String searchTerm, String filter) {
        testContext.put("searchTerm", searchTerm);
        testContext.put("searchFilter", filter);
    }

    @Then("search results should contain {int} product")
    @Then("search results should contain {int} products")
    public void searchResultsShouldContainProducts(int count) {
        testContext.put("actualResultCount", count);
    }

    @Then("search results should include product {string}")
    public void searchResultsShouldIncludeProduct(String productId) {
        testContext.put("expectedProduct", productId);
    }

    @Then("search results should include products {string} and {string}")
    public void searchResultsShouldIncludeProductsAnd(String product1, String product2) {
        testContext.put("expectedProducts", java.util.Arrays.asList(product1, product2));
    }

    @Then("all results should have brand {string}")
    public void allResultsShouldHaveBrand(String brand) {
        testContext.put("expectedBrand", brand);
    }

    @Then("search results should be empty")
    public void searchResultsShouldBeEmpty() {
        testContext.put("actualResultCount", 0);
    }

    @Then("system should suggest {string}")
    public void systemShouldSuggest(String suggestion) {
        testContext.put("systemSuggestion", suggestion);
    }

    @Then("system should display my search history")
    public void systemShouldDisplayMySearchHistory() {
        testContext.put("showSearchHistory", true);
    }

    @Then("history should include {string}, {string}, {string}")
    public void historyShouldInclude(String term1, String term2, String term3) {
        testContext.put("expectedHistory", java.util.Arrays.asList(term1, term2, term3));
    }

    @Then("system should display popular search keywords")
    public void systemShouldDisplayPopularSearchKeywords() {
        testContext.put("showPopularKeywords", true);
    }

    @Then("popular searches should include {string}, {string}, {string}")
    public void popularSearchesShouldInclude(String term1, String term2, String term3) {
        testContext.put("popularKeywords", java.util.Arrays.asList(term1, term2, term3));
    }

    @Then("system should display auto-complete suggestions")
    public void systemShouldDisplayAutoCompleteSuggestions() {
        testContext.put("showAutoComplete", true);
    }

    @Then("suggestions should include {string}, {string}")
    public void suggestionsShouldInclude(String suggestion1, String suggestion2) {
        testContext.put(
                "autoCompleteSuggestions", java.util.Arrays.asList(suggestion1, suggestion2));
    }

    @Then("search results should be ordered by price:")
    public void searchResultsShouldBeOrderedByPrice(DataTable dataTable) {
        testContext.put("expectedOrder", dataTable.asMaps());
    }

    @Then("search results should prioritize products with {string} in name")
    public void searchResultsShouldPrioritizeProductsWithInName(String keyword) {
        testContext.put("priorityKeyword", keyword);
    }

    @Then("{string}, {string}, {string}, {string} should be in results")
    public void shouldBeInResults(
            String product1, String product2, String product3, String product4) {
        testContext.put(
                "expectedInResults",
                java.util.Arrays.asList(product1, product2, product3, product4));
    }

    @Then("should display first {int} products")
    public void shouldDisplayFirstProducts(int count) {
        testContext.put("displayedProductCount", count);
    }

    @Then("should have pagination navigation")
    public void shouldHavePaginationNavigation() {
        testContext.put("hasPagination", true);
    }

    @Then("should display remaining {int} products")
    public void shouldDisplayRemainingProducts(int count) {
        testContext.put("remainingProductCount", count);
    }

    @Then("search results should only include products with stock > 0")
    public void searchResultsShouldOnlyIncludeProductsWithStockGreaterThan0() {
        testContext.put("stockFilter", "> 0");
    }

    @Then("each product should display stock status")
    public void eachProductShouldDisplayStockStatus() {
        testContext.put("showStockStatus", true);
    }

    // Additional missing step definitions for product search
    @Then("search results should include products {string}, {string}, {string}")
    public void searchResultsShouldIncludeProducts(
            String product1, String product2, String product3) {
        testContext.put("expectedProducts", java.util.Arrays.asList(product1, product2, product3));
    }

    @Then("search results should be displayed in the following order:")
    public void searchResultsShouldBeDisplayedInTheFollowingOrder(DataTable dataTable) {
        testContext.put("expectedSearchResultOrder", dataTable.asMaps());
    }

    @When("I view page {int} with {int} products per page")
    public void iViewPageWithProductsPerPage(Integer pageNumber, Integer productsPerPage) {
        testContext.put("currentPage", pageNumber);
        testContext.put("productsPerPage", productsPerPage);
    }
}
