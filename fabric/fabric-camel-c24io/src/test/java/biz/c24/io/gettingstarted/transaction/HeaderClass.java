package biz.c24.io.gettingstarted.transaction;


/**
 * The Header complex data type.
 * 
 * @author C24 Integration Objects;
 * @see biz.c24.io.gettingstarted.transaction.Header
 **/
public class HeaderClass extends biz.c24.io.api.data.ComplexDataType 
{
    protected static HeaderClass instance;
    private static boolean initialized;

    /**
     * Singleton constructor - use {@link #getInstance()} instead.
     **/
    protected HeaderClass()
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
            synchronized (biz.c24.io.gettingstarted.transaction.HeaderClass.class)
            {
                if (instance == null)
                {
                    instance = new biz.c24.io.gettingstarted.transaction.HeaderClass();
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
        setName("Header");
        setModel(biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance());
        setValidObjectClass(biz.c24.io.gettingstarted.transaction.Header.class);
        setContentModel(biz.c24.io.api.data.ContentModelEnum.SEQUENCE);
        biz.c24.io.api.data.Element element = null;
        setTerminator(new String[] {new String(new char[] {0xd, 0xa})});
        setFormatType(biz.c24.io.api.data.FormatTypeEnum.DELIMITED);
        setDelimiter(new String[] {new String(new char[] {0x2c})});
        setDelimiterLocation(biz.c24.io.api.data.DelimiterLocationEnum.INFIX);
        setDelimiterPlaceholder(true);
        setDelimiterFieldWrapper(new String[] {new String(new char[] {0x22})});
        addElementDecl(element = new biz.c24.io.api.data.Element("Name", 1, 1, biz.c24.io.gettingstarted.transaction.HeaderClass.NameClass.class, biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance()));
        addElementDecl(element = new biz.c24.io.api.data.Element("CardNumber", 1, 1, biz.c24.io.gettingstarted.transaction.HeaderClass.CardNumberClass.class, biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance()));
        element.setDatabaseColumnName("Card Number");
        addElementDecl(element = new biz.c24.io.api.data.Element("ExpiryDate", 1, 1, biz.c24.io.gettingstarted.transaction.HeaderClass.ExpiryDateClass.class, biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance()));
        element.setDatabaseColumnName("Expiry Date");
        addElementDecl(element = new biz.c24.io.api.data.Element("Amount", 1, 1, biz.c24.io.gettingstarted.transaction.HeaderClass.AmountClass.class, biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance()));
        addElementDecl(element = new biz.c24.io.api.data.Element("Currency", 1, 1, biz.c24.io.gettingstarted.transaction.HeaderClass.CurrencyClass.class, biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance()));
        addElementDecl(element = new biz.c24.io.api.data.Element("TransactionDate", 1, 1, biz.c24.io.gettingstarted.transaction.HeaderClass.TransactionDateClass.class, biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance()));
        element.setDatabaseColumnName("Transaction Date");
        addElementDecl(element = new biz.c24.io.api.data.Element("Commission", 1, 1, biz.c24.io.gettingstarted.transaction.HeaderClass.CommissionClass.class, biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance()));
        addElementDecl(element = new biz.c24.io.api.data.Element("VendorID", 1, 1, biz.c24.io.gettingstarted.transaction.HeaderClass.VendorIDClass.class, biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance()));
        element.setDatabaseColumnName("Vendor ID");
        addElementDecl(element = new biz.c24.io.api.data.Element("Country", 1, 1, biz.c24.io.gettingstarted.transaction.HeaderClass.CountryClass.class, biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance()));
    }

    private java.lang.Object readResolve()
    {
        return getInstance();
    }

    /**
     * The Amount atomic simple data type.
     * 
     * @author C24 Integration Objects;
     **/
    public static class AmountClass extends biz.c24.io.api.data.GenericStringDataType 
    {
        protected static biz.c24.io.gettingstarted.transaction.HeaderClass.AmountClass instance;
        private static boolean initialized;

        /**
         * Singleton constructor - use {@link #getInstance()} instead.
         **/
        protected AmountClass()
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
                synchronized (biz.c24.io.gettingstarted.transaction.HeaderClass.AmountClass.class)
                {
                    if (instance == null)
                    {
                        instance = new biz.c24.io.gettingstarted.transaction.HeaderClass.AmountClass();
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
            setName("Amount");
            setModel(biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance());
            setLocal(true);
            setMinLength(0);
        }

        private java.lang.Object readResolve()
        {
            return getInstance();
        }

    }


    /**
     * The Card Number atomic simple data type.
     * 
     * @author C24 Integration Objects;
     **/
    public static class CardNumberClass extends biz.c24.io.api.data.GenericStringDataType 
    {
        protected static biz.c24.io.gettingstarted.transaction.HeaderClass.CardNumberClass instance;
        private static boolean initialized;

        /**
         * Singleton constructor - use {@link #getInstance()} instead.
         **/
        protected CardNumberClass()
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
                synchronized (biz.c24.io.gettingstarted.transaction.HeaderClass.CardNumberClass.class)
                {
                    if (instance == null)
                    {
                        instance = new biz.c24.io.gettingstarted.transaction.HeaderClass.CardNumberClass();
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
            setName("CardNumber");
            setModel(biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance());
            setDatabaseColumnName("Card Number");
            setLocal(true);
            setMinLength(0);
        }

        private java.lang.Object readResolve()
        {
            return getInstance();
        }

    }


    /**
     * The Commission atomic simple data type.
     * 
     * @author C24 Integration Objects;
     **/
    public static class CommissionClass extends biz.c24.io.api.data.GenericStringDataType 
    {
        protected static biz.c24.io.gettingstarted.transaction.HeaderClass.CommissionClass instance;
        private static boolean initialized;

        /**
         * Singleton constructor - use {@link #getInstance()} instead.
         **/
        protected CommissionClass()
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
                synchronized (biz.c24.io.gettingstarted.transaction.HeaderClass.CommissionClass.class)
                {
                    if (instance == null)
                    {
                        instance = new biz.c24.io.gettingstarted.transaction.HeaderClass.CommissionClass();
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
            setName("Commission");
            setModel(biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance());
            setLocal(true);
            setMinLength(0);
        }

        private java.lang.Object readResolve()
        {
            return getInstance();
        }

    }


    /**
     * The Country atomic simple data type.
     * 
     * @author C24 Integration Objects;
     **/
    public static class CountryClass extends biz.c24.io.api.data.GenericStringDataType 
    {
        protected static biz.c24.io.gettingstarted.transaction.HeaderClass.CountryClass instance;
        private static boolean initialized;

        /**
         * Singleton constructor - use {@link #getInstance()} instead.
         **/
        protected CountryClass()
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
                synchronized (biz.c24.io.gettingstarted.transaction.HeaderClass.CountryClass.class)
                {
                    if (instance == null)
                    {
                        instance = new biz.c24.io.gettingstarted.transaction.HeaderClass.CountryClass();
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
            setName("Country");
            setModel(biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance());
            setLocal(true);
            setMinLength(0);
        }

        private java.lang.Object readResolve()
        {
            return getInstance();
        }

    }


    /**
     * The Currency atomic simple data type.
     * 
     * @author C24 Integration Objects;
     **/
    public static class CurrencyClass extends biz.c24.io.api.data.GenericStringDataType 
    {
        protected static biz.c24.io.gettingstarted.transaction.HeaderClass.CurrencyClass instance;
        private static boolean initialized;

        /**
         * Singleton constructor - use {@link #getInstance()} instead.
         **/
        protected CurrencyClass()
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
                synchronized (biz.c24.io.gettingstarted.transaction.HeaderClass.CurrencyClass.class)
                {
                    if (instance == null)
                    {
                        instance = new biz.c24.io.gettingstarted.transaction.HeaderClass.CurrencyClass();
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
            setName("Currency");
            setModel(biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance());
            setLocal(true);
            setMinLength(0);
        }

        private java.lang.Object readResolve()
        {
            return getInstance();
        }

    }


    /**
     * The Expiry Date atomic simple data type.
     * 
     * @author C24 Integration Objects;
     **/
    public static class ExpiryDateClass extends biz.c24.io.api.data.GenericStringDataType 
    {
        protected static biz.c24.io.gettingstarted.transaction.HeaderClass.ExpiryDateClass instance;
        private static boolean initialized;

        /**
         * Singleton constructor - use {@link #getInstance()} instead.
         **/
        protected ExpiryDateClass()
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
                synchronized (biz.c24.io.gettingstarted.transaction.HeaderClass.ExpiryDateClass.class)
                {
                    if (instance == null)
                    {
                        instance = new biz.c24.io.gettingstarted.transaction.HeaderClass.ExpiryDateClass();
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
            setName("ExpiryDate");
            setModel(biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance());
            setDatabaseColumnName("Expiry Date");
            setLocal(true);
            setMinLength(0);
        }

        private java.lang.Object readResolve()
        {
            return getInstance();
        }

    }


    /**
     * The Name atomic simple data type.
     * 
     * @author C24 Integration Objects;
     **/
    public static class NameClass extends biz.c24.io.api.data.GenericStringDataType 
    {
        protected static biz.c24.io.gettingstarted.transaction.HeaderClass.NameClass instance;
        private static boolean initialized;

        /**
         * Singleton constructor - use {@link #getInstance()} instead.
         **/
        protected NameClass()
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
                synchronized (biz.c24.io.gettingstarted.transaction.HeaderClass.NameClass.class)
                {
                    if (instance == null)
                    {
                        instance = new biz.c24.io.gettingstarted.transaction.HeaderClass.NameClass();
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
            setName("Name");
            setModel(biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance());
            setLocal(true);
            setMinLength(0);
        }

        private java.lang.Object readResolve()
        {
            return getInstance();
        }

    }


    /**
     * The Transaction Date atomic simple data type.
     * 
     * @author C24 Integration Objects;
     **/
    public static class TransactionDateClass extends biz.c24.io.api.data.GenericStringDataType 
    {
        protected static biz.c24.io.gettingstarted.transaction.HeaderClass.TransactionDateClass instance;
        private static boolean initialized;

        /**
         * Singleton constructor - use {@link #getInstance()} instead.
         **/
        protected TransactionDateClass()
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
                synchronized (biz.c24.io.gettingstarted.transaction.HeaderClass.TransactionDateClass.class)
                {
                    if (instance == null)
                    {
                        instance = new biz.c24.io.gettingstarted.transaction.HeaderClass.TransactionDateClass();
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
            setName("TransactionDate");
            setModel(biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance());
            setDatabaseColumnName("Transaction Date");
            setLocal(true);
            setMinLength(0);
        }

        private java.lang.Object readResolve()
        {
            return getInstance();
        }

    }


    /**
     * The Vendor ID atomic simple data type.
     * 
     * @author C24 Integration Objects;
     **/
    public static class VendorIDClass extends biz.c24.io.api.data.GenericStringDataType 
    {
        protected static biz.c24.io.gettingstarted.transaction.HeaderClass.VendorIDClass instance;
        private static boolean initialized;

        /**
         * Singleton constructor - use {@link #getInstance()} instead.
         **/
        protected VendorIDClass()
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
                synchronized (biz.c24.io.gettingstarted.transaction.HeaderClass.VendorIDClass.class)
                {
                    if (instance == null)
                    {
                        instance = new biz.c24.io.gettingstarted.transaction.HeaderClass.VendorIDClass();
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
            setName("VendorID");
            setModel(biz.c24.io.gettingstarted.transaction.TransactionsDataModel.getInstance());
            setDatabaseColumnName("Vendor ID");
            setLocal(true);
            setMinLength(0);
        }

        private java.lang.Object readResolve()
        {
            return getInstance();
        }

    }

}
