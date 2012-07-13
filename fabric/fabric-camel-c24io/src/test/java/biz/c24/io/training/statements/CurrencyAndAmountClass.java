package biz.c24.io.training.statements;


/**
 * The CurrencyAndAmount complex data type.
 * 
 * @author C24 Integration Objects;
 * @see biz.c24.io.training.statements.CurrencyAndAmount
 **/
public class CurrencyAndAmountClass extends biz.c24.io.api.data.ComplexDataType 
{
    protected static CurrencyAndAmountClass instance;
    private static boolean initialized;

    /**
     * Singleton constructor - use {@link #getInstance()} instead.
     **/
    protected CurrencyAndAmountClass()
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
            synchronized (biz.c24.io.training.statements.CurrencyAndAmountClass.class)
            {
                if (instance == null)
                {
                    instance = new biz.c24.io.training.statements.CurrencyAndAmountClass();
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
        setName("CurrencyAndAmount");
        setModel(biz.c24.io.training.statements.StatementsDataModel.getInstance());
        setValidObjectClass(biz.c24.io.training.statements.CurrencyAndAmount.class);
        setContentModel(biz.c24.io.api.data.ContentModelEnum.SEQUENCE);
        setContentType((biz.c24.io.api.data.SimpleDataType) biz.c24.io.training.statements.CurrencyAndAmount_SimpleTypeClass.getInstance());
        biz.c24.io.api.data.Attribute attribute = null;
        addAttrDecl(attribute = new biz.c24.io.api.data.Attribute("Ccy", 1, 1, biz.c24.io.training.statements.CurrencyCodeClass.class, biz.c24.io.training.statements.StatementsDataModel.getInstance()));
    }

    private java.lang.Object readResolve()
    {
        return getInstance();
    }

}
