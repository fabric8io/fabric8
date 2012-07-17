package biz.c24.io.gettingstarted.transaction;


/**
 * The rowCheckRule validation rule.
 * 
 * @author C24 Integration Objects;
 **/
public class RowCheckRuleRule extends biz.c24.io.api.data.XPathRule 
{
    private static biz.c24.io.api.data.ValidationRule instance;

    private RowCheckRuleRule()
    {
        setStatement(new biz.c24.io.api.data.XPathStatement("/Transactions/RowCount/Value!=count(/Transactions/CustomerDetails)", false, false));
        setErrorMessage("Invalid row count");
        setName("rowCheckRule");
        setSeverity(biz.c24.io.api.data.ValidationSeverityEnum.ERROR);
    }

    public static biz.c24.io.api.data.ValidationRule getInstance()
    {
        if (instance == null)
            instance = new biz.c24.io.gettingstarted.transaction.RowCheckRuleRule();
        
        return instance;
    }

}
