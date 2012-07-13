package biz.c24.io.gettingstarted.transaction;


/**
 * The Transactions data model.
 * 
 * @author C24 Integration Objects;
 **/
public class TransactionsDataModel extends biz.c24.io.api.data.DataModel 
{
    private static TransactionsDataModel instance;
    private static boolean initialized;

    protected TransactionsDataModel(boolean init)
    {
        if (init)
            init();
    }

    /**
     * Gets the singleton instance.
     * @return The instance.
     **/
    public static biz.c24.io.gettingstarted.transaction.TransactionsDataModel getInstance()
    {
        if (!initialized)
        {
            synchronized (biz.c24.io.gettingstarted.transaction.TransactionsDataModel.class)
            {
                if (instance == null)
                {
                    instance = new biz.c24.io.gettingstarted.transaction.TransactionsDataModel(false);
                    instance.init();
                    initialized = true;
                }
            }
        }
        return instance;
    }

    private void init()
    {
        setName("Transactions");
        setTargetNamespace("http://www.c24.biz/io/GettingStarted/Transaction");
        setElementFormDefault(biz.c24.io.api.data.FormEnum.QUALIFIED);
        setAttributeFormDefault(biz.c24.io.api.data.FormEnum.UNQUALIFIED);
        setIdGeneratorMethodDefault(biz.c24.io.api.data.IDGeneratorMethodEnum.NATIVE);
    }

}
