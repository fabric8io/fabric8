package biz.c24.io.training.statements;


/**
 * The AddressType2Code atomic simple data type.
 * 
 * @author C24 Integration Objects;
 **/
public class AddressType2CodeClass extends biz.c24.io.api.data.GenericStringDataType 
{
    protected static AddressType2CodeClass instance;
    private static boolean initialized;

    /**
     * Singleton constructor - use {@link #getInstance()} instead.
     **/
    protected AddressType2CodeClass()
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
            synchronized (biz.c24.io.training.statements.AddressType2CodeClass.class)
            {
                if (instance == null)
                {
                    instance = new biz.c24.io.training.statements.AddressType2CodeClass();
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
        setName("AddressType2Code");
        setModel(biz.c24.io.training.statements.StatementsDataModel.getInstance());
        addValidator(biz.c24.io.training.statements.AddressType2CodeClass.AddressType2Code1Enum.getInstance());
    }

    private java.lang.Object readResolve()
    {
        return getInstance();
    }

    /**
     * The AddressType2Code enumeration.
     * 
     * @author C24 Integration Objects;
     **/
    public static class AddressType2Code1Enum extends biz.c24.io.api.data.DefaultEnumeration 
    {
        private static biz.c24.io.api.data.Enumeration instance;

        private AddressType2Code1Enum()
        {
            setName("AddressType2Code");
            addEntry("ADDR", "", null);
            addEntry("PBOX", "", null);
            addEntry("HOME", "", null);
            addEntry("BIZZ", "", null);
            addEntry("MLTO", "", null);
            addEntry("DLVY", "", null);
        }

        public static biz.c24.io.api.data.Enumeration getInstance()
        {
            if (instance == null)
                instance = new biz.c24.io.training.statements.AddressType2CodeClass.AddressType2Code1Enum();
            
            return instance;
        }

    }

}
