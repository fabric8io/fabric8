package biz.c24.io.training.statements;

import java.util.Arrays;

/**
 * CurrencyAndAmount. <p/>
 * This object is composed of the following <i>attribute</i>:
 * <ul>
 * <li><b>Ccy</b> of type {@link java.lang.String} (required)</li>
 * </ul>
 * @author C24 Integration Objects;
 * @see biz.c24.io.training.statements.CurrencyAndAmountClass
 **/
public class CurrencyAndAmount extends biz.c24.io.api.data.ComplexDataObject 
{
    private static final java.lang.String[] NATIVE_ATTRIBUTES = new String[] {"Ccy"};
    private java.lang.String ccy;

    /**
     * Constructs a new instance defined by the default element.
     **/
    public CurrencyAndAmount()
    {
        this(biz.c24.io.training.statements.CurrencyAndAmountClass.getInstance().getNullDefiningElementDecl());
    }

    /**
     * Constructs a new instance defined by the specified element.
     * @param definingElementDecl The element which defines the object.
     **/
    public CurrencyAndAmount(biz.c24.io.api.data.Element definingElementDecl)
    {
        super(definingElementDecl);
    }

    /**
     * Constructs a new instance defined by the specified element and type.
     * @param definingElementDecl The element which defines the object.
     * @param type The type which defines the object.
     **/
    public CurrencyAndAmount(biz.c24.io.api.data.Element definingElementDecl, biz.c24.io.api.data.ComplexDataType type)
    {
        super(definingElementDecl, type);
    }

    /**
     * Constructs a new instance cloned from the specified object.
     * @param clone The object to be cloned.
     **/
    public CurrencyAndAmount(biz.c24.io.training.statements.CurrencyAndAmount clone)
    {
        super(clone);
    }

    /**
     * Creates and returns a shallow clone of this object.
     * @see #cloneDeep()
     **/
    public java.lang.Object clone()
    {
        return new biz.c24.io.training.statements.CurrencyAndAmount(this);
    }

    /**
     * Creates and returns a deep clone of this object.
     * @return The new object.
     * @see #clone()
     **/
    public biz.c24.io.api.data.ComplexDataObject cloneDeep() throws java.lang.CloneNotSupportedException
    {
        biz.c24.io.api.data.ComplexDataObject obj = new biz.c24.io.training.statements.CurrencyAndAmount(this);
        cloneDeep(obj);
        return obj;
    }

    protected void cloneDeep(biz.c24.io.api.data.ComplexDataObject clone) throws java.lang.CloneNotSupportedException
    {
        biz.c24.io.training.statements.CurrencyAndAmount obj = (biz.c24.io.training.statements.CurrencyAndAmount) clone;
        obj.ccy = (java.lang.String) biz.c24.io.api.Utils.cloneDeep(this.ccy, obj, "Ccy");
    }

    public boolean equals(java.lang.Object obj)
    {
        if(obj instanceof biz.c24.io.api.data.ComplexDataObject)
            return equalContents((biz.c24.io.api.data.ComplexDataObject) obj, true, true, true, true);
        else
            return obj.equals(this);
    }

    /**
     * Gets the attribute called <code>name</code>.<p>
     * The legal value(s) for <code>name</code> are: <b>ccy</b>.
     **/
    public java.lang.Object getAttr(java.lang.String name)
    {
        switch (Arrays.binarySearch(NATIVE_ATTRIBUTES, name))
        {
            case 0:
                return this.ccy;
            default:
                return super.getAttr(name);
        }
    }

    /**
     * Gets the value of Ccy (required).
     **/
    public java.lang.String getCcy()
    {
        return this.ccy;
    }

    public int getTotalAttrCount()
    {
        int count = super.getTotalAttrCount();
        count += this.ccy == null ? 0 : 1;
        return count;
    }

    /**
     * Get content.
     * @return The value.
     **/
    public float getValue()
    {
        return biz.c24.io.api.Utils.floatValue(getContent());
    }

    public int hashCode()
    {
        return this.toString().length();
    }

    /**
     * Returns whether the attribute called <code>name</code> is present.<p>
     * The legal value(s) for <code>name</code> are defined in {@link #getAttr}.
     **/
    public boolean isAttrPresent(java.lang.String name)
    {
        switch (Arrays.binarySearch(NATIVE_ATTRIBUTES, name))
        {
            case 0:
                return this.ccy == null ? false : true;
            default:
                return super.isAttrPresent(name);
        }
    }

    /**
     * Removes the attribute called <code>name</code>.<p>
     * The legal value(s) for <code>name</code> are defined in {@link #getAttr}.
     **/
    public void removeAttr(java.lang.String name)
    {
        switch (Arrays.binarySearch(NATIVE_ATTRIBUTES, name))
        {
            case 0:
                this.ccy = null;
                return;
            default:
                super.removeAttr(name);
        }
    }

    /**
     * Sets the attribute called <code>name</code> to <code>value<code>.<p>
     * The legal value(s) for <code>name</code> are defined in {@link #getAttr}.
     **/
    public void setAttr(java.lang.String name, java.lang.Object value)
    {
        switch (Arrays.binarySearch(NATIVE_ATTRIBUTES, name))
        {
            case 0:
                setCcy((java.lang.String) value);
                return;
            default:
                super.setAttr(name, value);
        }
    }

    /**
     * Sets the value of Ccy (required).
     * @param value The value to use.
     **/
    public void setCcy(java.lang.String value)
    {
        this.ccy = value;
    }

    public void setContent(java.lang.Object value)
    {
        if (value instanceof java.lang.Float || value == null)
            super.setContent(value);
        else
            throw new ClassCastException("Expecting instance of float");
    }

    /**
     * Set content.
     * @param value The value to use.
     **/
    public void setValue(float value)
    {
        setContent(new java.lang.Float((float) value));
    }

    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException
    {
        out.writeObject(this.ccy);
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, java.lang.ClassNotFoundException
    {
        this.ccy = (java.lang.String) in.readObject();
    }

}
