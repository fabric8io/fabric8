package biz.c24.io.training.statements;


/**
 * The CurrencyAndAmount_SimpleType atomic simple data type.
 * 
 * @author C24 Integration Objects;
 **/
public class CurrencyAndAmount_SimpleTypeClass extends biz.c24.io.api.data.FloatDataType 
{
    protected static CurrencyAndAmount_SimpleTypeClass instance;
    private static boolean initialized;

    /**
     * Singleton constructor - use {@link #getInstance()} instead.
     **/
    protected CurrencyAndAmount_SimpleTypeClass()
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
            synchronized (biz.c24.io.training.statements.CurrencyAndAmount_SimpleTypeClass.class)
            {
                if (instance == null)
                {
                    instance = new biz.c24.io.training.statements.CurrencyAndAmount_SimpleTypeClass();
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
        setName("CurrencyAndAmount_SimpleType");
        setModel(biz.c24.io.training.statements.StatementsDataModel.getInstance());
        setMaxFractionDigits(5);
        setMaxTotalDigits(18);
        setMinInclusive(new java.lang.Float("0"));
        addDerivedType("CurrencyAndAmount", biz.c24.io.training.statements.CurrencyAndAmountClass.getInstance());
    }

    private java.lang.Object readResolve()
    {
        return getInstance();
    }

}
