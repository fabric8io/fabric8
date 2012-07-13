package biz.c24.io.gettingstarted.transaction;


/**
 * The Transactions complex data type.
 * 
 * @author C24 Integration Objects;
 * @see biz.c24.io.gettingstarted.transaction.Transactions
 **/
public class TransactionsClass extends biz.c24.io.api.data.ComplexDataType 
{
    protected static TransactionsClass instance;
    private static boolean initialized;

    /**
     * Singleton constructor - use {@link #getInstance()} instead.
     **/
    protected TransactionsClass()
    {
    }

    /**
     * Gets the singleton instance of this type.
     * @return The type, or its supertype if present.
     **/
    public static biz.c24.io.api.data.DataType getInstance()
    {
        if (!initialized)
        {
            synchronized (biz.c24.io.gettingstarted.transaction.TransactionsClass.class)
            {
                if (instance == null)
                {
                    instance = new biz.c24.io.gettingstarted.transaction.TransactionsClass();
                    instance.init();
                    initialized = true;
                }
            }
        }
        return instance;
    }

    /**
     * Called internally to initialize this type.
     **/
    protected void init()
    {
        setName("Transactions");
        setModel(biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance());
        setValidObjectClass(biz.c24.io.gettingstarted.transaction.Transactions.class);
        setContentModel(biz.c24.io.api.data.ContentModelEnum.SEQUENCE);
        biz.c24.io.api.data.Element element = null;
        addValidator(biz.c24.io.gettingstarted.transaction.RowCheckRuleRule.getInstance());
        addElementDecl(element = new biz.c24.io.api.data.Element("Header", 1, 1, biz.c24.io.gettingstarted.transaction.HeaderClass.class, biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance()));
        addElementDecl(element = new biz.c24.io.api.data.Element("CustomerDetails", 1, biz.c24.io.api.data.DataComponent.CARDINALITY_UNBOUNDED, biz.c24.io.gettingstarted.transaction.CustomerDetailsClass.class, biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance()));
        element.setDatabaseColumnName("Customer Details");
        addElementDecl(element = new biz.c24.io.api.data.Element("RowCount", 1, 1, biz.c24.io.gettingstarted.transaction.RowCountClass.class, biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance()));
        element.setDatabaseColumnName("Row Count");
    }

    private java.lang.Object readResolve()
    {
        return getInstance();
    }

}
