package biz.c24.io.gettingstarted.transaction;


/**
 * The Row Count complex data type.
 * 
 * @author C24 Integration Objects;
 * @see biz.c24.io.gettingstarted.transaction.RowCount
 **/
public class RowCountClass extends biz.c24.io.api.data.ComplexDataType 
{
    protected static RowCountClass instance;
    private static boolean initialized;

    /**
     * Singleton constructor - use {@link #getInstance()} instead.
     **/
    protected RowCountClass()
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
            synchronized (biz.c24.io.gettingstarted.transaction.RowCountClass.class)
            {
                if (instance == null)
                {
                    instance = new biz.c24.io.gettingstarted.transaction.RowCountClass();
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
        setName("RowCount");
        setModel(biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance());
        setDatabaseColumnName("Row Count");
        setValidObjectClass(biz.c24.io.gettingstarted.transaction.RowCount.class);
        setContentModel(biz.c24.io.api.data.ContentModelEnum.SEQUENCE);
        biz.c24.io.api.data.Element element = null;
        setTerminator(new String[] {new String(new char[] {0xd, 0xa})});
        setFormatType(biz.c24.io.api.data.FormatTypeEnum.FIXED);
        addElementDecl(element = new biz.c24.io.api.data.Element("Prefix", 1, 1, biz.c24.io.gettingstarted.transaction.RowCountClass.PrefixClass.class, biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance()));
        addElementDecl(element = new biz.c24.io.api.data.Element("Value", 0, 1, biz.c24.io.gettingstarted.transaction.RowCountClass.ValueClass.class, biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance()));
    }

    private java.lang.Object readResolve()
    {
        return getInstance();
    }

    /**
     * The Prefix atomic simple data type.
     * 
     * @author C24 Integration Objects;
     **/
    public static class PrefixClass extends biz.c24.io.api.data.GenericStringDataType 
    {
        protected static biz.c24.io.gettingstarted.transaction.RowCountClass.PrefixClass instance;
        private static boolean initialized;

        /**
         * Singleton constructor - use {@link #getInstance()} instead.
         **/
        protected PrefixClass()
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
                synchronized (biz.c24.io.gettingstarted.transaction.RowCountClass.PrefixClass.class)
                {
                    if (instance == null)
                    {
                        instance = new biz.c24.io.gettingstarted.transaction.RowCountClass.PrefixClass();
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
            setName("Prefix");
            setModel(biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance());
            setLocal(true);
            setMinLength(9);
            setMaxLength(9);
        }

        private java.lang.Object readResolve()
        {
            return getInstance();
        }

    }


    /**
     * The Value atomic simple data type.
     * 
     * @author C24 Integration Objects;
     **/
    public static class ValueClass extends biz.c24.io.api.data.LongDataType 
    {
        protected static biz.c24.io.gettingstarted.transaction.RowCountClass.ValueClass instance;
        private static boolean initialized;

        /**
         * Singleton constructor - use {@link #getInstance()} instead.
         **/
        protected ValueClass()
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
                synchronized (biz.c24.io.gettingstarted.transaction.RowCountClass.ValueClass.class)
                {
                    if (instance == null)
                    {
                        instance = new biz.c24.io.gettingstarted.transaction.RowCountClass.ValueClass();
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
            setName("Value");
            setModel(biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance());
            setLocal(true);
            setAllFormatPatterns(new String[] {new String(new char[] {0x23, 0x3b, 0x27, 0x2d, 0x27, 0x23})});
            setMaxTotalDigits(1);
        }

        private java.lang.Object readResolve()
        {
            return getInstance();
        }

    }

}
