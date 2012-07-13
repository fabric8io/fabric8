package biz.c24.io.training.statements;

import java.util.Arrays;

/**
 * StatementLine. <p/>
 * This object is composed of the following <i>elements</i>:
 * <ul>
 * <li><b>PostingDate</b> of type {@link biz.c24.io.api.data.ISO8601Date} (1)</li>
 * <li><b>ValueDate</b> of type {@link biz.c24.io.api.data.ISO8601Date} (1)</li>
 * <li><b>DrCr</b> of type {@link java.lang.String} (1)</li>
 * <li><b>TxAmount</b> of type {@link biz.c24.io.training.statements.CurrencyAndAmount} (1)</li>
 * <li><b>PostingNarrative</b> of type {@link java.lang.String} (1)</li>
 * </ul>
 * @author C24 Integration Objects;
 * @see biz.c24.io.training.statements.StatementLineClass
 **/
public class StatementLine extends biz.c24.io.api.data.ComplexDataObject 
{
    private static final java.lang.String[] NATIVE_ELEMENTS = new String[] {"DrCr", "PostingDate", "PostingNarrative", "TxAmount", "ValueDate"};
    private biz.c24.io.api.data.ISO8601Date postingDate;
    private biz.c24.io.api.data.ISO8601Date valueDate;
    private java.lang.String drCr;
    private biz.c24.io.training.statements.CurrencyAndAmount txAmount;
    private java.lang.String postingNarrative;

    /**
     * Constructs a new instance defined by the default element.
     **/
    public StatementLine()
    {
        this(biz.c24.io.training.statements.StatementLineClass.getInstance().getNullDefiningElementDecl());
    }

    /**
     * Constructs a new instance defined by the specified element.
     * @param definingElementDecl The element which defines the object.
     **/
    public StatementLine(biz.c24.io.api.data.Element definingElementDecl)
    {
        super(definingElementDecl);
    }

    /**
     * Constructs a new instance defined by the specified element and type.
     * @param definingElementDecl The element which defines the object.
     * @param type The type which defines the object.
     **/
    public StatementLine(biz.c24.io.api.data.Element definingElementDecl, biz.c24.io.api.data.ComplexDataType type)
    {
        super(definingElementDecl, type);
    }

    /**
     * Constructs a new instance cloned from the specified object.
     * @param clone The object to be cloned.
     **/
    public StatementLine(biz.c24.io.training.statements.StatementLine clone)
    {
        super(clone);
    }

    /**
     * Adds <code>value</code> as an element called <code>name</code>.<p>
     * The legal value(s) for <code>name</code> are defined in {@link #getElement}.
     **/
    public void addElement(java.lang.String name, java.lang.Object value)
    {
        name = makeSubstitution(name, -1);
        switch (Arrays.binarySearch(NATIVE_ELEMENTS, name))
        {
            case 1:
                setPostingDate((biz.c24.io.api.data.ISO8601Date) value);
                return;
            case 4:
                setValueDate((biz.c24.io.api.data.ISO8601Date) value);
                return;
            case 0:
                setDrCr((java.lang.String) value);
                return;
            case 3:
                setTxAmount((biz.c24.io.training.statements.CurrencyAndAmount) value);
                return;
            case 2:
                setPostingNarrative((java.lang.String) value);
                return;
            default:
                super.addElement(name, value);
        }
    }

    /**
     * Creates and returns a shallow clone of this object.
     * @see #cloneDeep()
     **/
    public java.lang.Object clone()
    {
        return new biz.c24.io.training.statements.StatementLine(this);
    }

    /**
     * Creates and returns a deep clone of this object.
     * @return The new object.
     * @see #clone()
     **/
    public biz.c24.io.api.data.ComplexDataObject cloneDeep() throws java.lang.CloneNotSupportedException
    {
        biz.c24.io.api.data.ComplexDataObject obj = new biz.c24.io.training.statements.StatementLine(this);
        cloneDeep(obj);
        return obj;
    }

    protected void cloneDeep(biz.c24.io.api.data.ComplexDataObject clone) throws java.lang.CloneNotSupportedException
    {
        biz.c24.io.training.statements.StatementLine obj = (biz.c24.io.training.statements.StatementLine) clone;
        obj.postingDate = (biz.c24.io.api.data.ISO8601Date) biz.c24.io.api.Utils.cloneDeep(this.postingDate, obj, "PostingDate");
        obj.valueDate = (biz.c24.io.api.data.ISO8601Date) biz.c24.io.api.Utils.cloneDeep(this.valueDate, obj, "ValueDate");
        obj.drCr = (java.lang.String) biz.c24.io.api.Utils.cloneDeep(this.drCr, obj, "DrCr");
        obj.txAmount = (biz.c24.io.training.statements.CurrencyAndAmount) biz.c24.io.api.Utils.cloneDeep(this.txAmount, obj, "TxAmount");
        obj.postingNarrative = (java.lang.String) biz.c24.io.api.Utils.cloneDeep(this.postingNarrative, obj, "PostingNarrative");
    }

    /**
     * Creates, adds and returns a new TxAmount (1).
     * @return The new value.
     **/
    public biz.c24.io.training.statements.CurrencyAndAmount createTxAmount()
    {
        biz.c24.io.training.statements.CurrencyAndAmount obj = (biz.c24.io.training.statements.CurrencyAndAmount) getElementDecl("TxAmount").createObject();
        setTxAmount(obj);
        return obj;
    }

    public boolean equals(java.lang.Object obj)
    {
        if(obj instanceof biz.c24.io.api.data.ComplexDataObject)
            return equalContents((biz.c24.io.api.data.ComplexDataObject) obj, true, true, true, true);
        else
            return obj.equals(this);
    }

    /**
     * Gets the value of DrCr (1).
     * @return The value.
     **/
    public java.lang.String getDrCr()
    {
        return this.drCr;
    }

    /**
     * Returns the element called <code>name</code> at <code>index</code>.<p>
     * The legal value(s) for <code>name</code> are: <b>postingDate, valueDate, drCr, txAmount, postingNarrative</b>.
     **/
    public java.lang.Object getElement(java.lang.String name, int index)
    {
        int i = Arrays.binarySearch(NATIVE_ELEMENTS, name);
        if (i < 0)
        {
            name = getSubstitute(name);
            i = Arrays.binarySearch(NATIVE_ELEMENTS, name);
        }
        switch (i)
        {
            case 1:
                return this.postingDate;
            case 4:
                return this.valueDate;
            case 0:
                return this.drCr;
            case 3:
                return this.txAmount;
            case 2:
                return this.postingNarrative;
            default:
                return super.getElement(name, index);
        }
    }

    /**
     * Returns the count of elements called <code>name</code>.<p>
     * The legal value(s) for <code>name</code> are defined in {@link #getElement}.
     **/
    public int getElementCount(java.lang.String name)
    {
        if (null == name) throw new NullPointerException(toString() + " was asked to calculate elements without name");
        int i = Arrays.binarySearch(NATIVE_ELEMENTS, name);
        if (i < 0)
        {
            name = getSubstitute(name);
            i = Arrays.binarySearch(NATIVE_ELEMENTS, name);
        }
        switch (i)
        {
            case 1:
                return this.postingDate == null ? 0 : 1;
            case 4:
                return this.valueDate == null ? 0 : 1;
            case 0:
                return this.drCr == null ? 0 : 1;
            case 3:
                return this.txAmount == null ? 0 : 1;
            case 2:
                return this.postingNarrative == null ? 0 : 1;
            default:
                return super.getElementCount(name);
        }
    }

    /**
     * Returns the element called <code>name</code> at <code>index</code>.<p>
     * The legal value(s) for <code>name</code> are defined in {@link #getElement}.
     **/
    public int getElementIndex(java.lang.String name, java.lang.Object element)
    {
        int i = Arrays.binarySearch(NATIVE_ELEMENTS, name);
        if (i < 0)
        {
            name = getSubstitute(name);
            i = Arrays.binarySearch(NATIVE_ELEMENTS, name);
        }
        switch (i)
        {
            case 1:
                return this.postingDate != null && this.postingDate.equals(element) ? 0 : -1;
            case 4:
                return this.valueDate != null && this.valueDate.equals(element) ? 0 : -1;
            case 0:
                return this.drCr != null && this.drCr.equals(element) ? 0 : -1;
            case 3:
                return this.txAmount != null && this.txAmount.equals(element) ? 0 : -1;
            case 2:
                return this.postingNarrative != null && this.postingNarrative.equals(element) ? 0 : -1;
            default:
                return super.getElementIndex(name, element);
        }
    }

    /**
     * Gets the value of PostingDate (1).
     * @return The value.
     **/
    public biz.c24.io.api.data.ISO8601Date getPostingDate()
    {
        return this.postingDate;
    }

    /**
     * Gets the value of PostingNarrative (1).
     * @return The value.
     **/
    public java.lang.String getPostingNarrative()
    {
        return this.postingNarrative;
    }

    public int getTotalElementCount()
    {
        int count = 0;
        count += this.postingDate == null ? 0 : 1;
        count += this.valueDate == null ? 0 : 1;
        count += this.drCr == null ? 0 : 1;
        count += this.txAmount == null ? 0 : 1;
        count += this.postingNarrative == null ? 0 : 1;
        return count;
    }

    /**
     * Gets the value of TxAmount (1).
     * @return The value.
     **/
    public biz.c24.io.training.statements.CurrencyAndAmount getTxAmount()
    {
        return this.txAmount;
    }

    /**
     * Gets the value of ValueDate (1).
     * @return The value.
     **/
    public biz.c24.io.api.data.ISO8601Date getValueDate()
    {
        return this.valueDate;
    }

    public int hashCode()
    {
        return this.toString().length();
    }

    /**
     * Removes the element called <code>name</code> at <code>index</code>.<p>
     * The legal value(s) for <code>name</code> are defined in {@link #getElement}.
     **/
    public void removeElement(java.lang.String name, int index)
    {
        name = unmakeSubstitution(name, index);
        switch (Arrays.binarySearch(NATIVE_ELEMENTS, name))
        {
            case 1:
                setPostingDate(null);
                return;
            case 4:
                setValueDate(null);
                return;
            case 0:
                setDrCr(null);
                return;
            case 3:
                setTxAmount(null);
                return;
            case 2:
                setPostingNarrative(null);
                return;
            default:
            super.removeElement(name, index);
        }
    }

    /**
     * Sets the value of DrCr (1).
     * @param value The new value.
     **/
    public void setDrCr(java.lang.String value)
    {
        this.drCr = value;
    }

    /**
     * Sets <code>value</code> as an element called <code>name</code> at <code>index</code>.<p>
     * The legal value(s) for <code>name</code> are defined in {@link #getElement}.
     **/
    public void setElement(java.lang.String name, int index, java.lang.Object value)
    {
        name = makeSubstitution(name, index);
        switch (Arrays.binarySearch(NATIVE_ELEMENTS, name))
        {
            case 1:
                setPostingDate((biz.c24.io.api.data.ISO8601Date) value);
                return;
            case 4:
                setValueDate((biz.c24.io.api.data.ISO8601Date) value);
                return;
            case 0:
                setDrCr((java.lang.String) value);
                return;
            case 3:
                setTxAmount((biz.c24.io.training.statements.CurrencyAndAmount) value);
                return;
            case 2:
                setPostingNarrative((java.lang.String) value);
                return;
            default:
                super.setElement(name, index, value);
        }
    }

    /**
     * Sets the value of PostingDate (1).
     * @param value The new value.
     **/
    public void setPostingDate(biz.c24.io.api.data.ISO8601Date value)
    {
        this.postingDate = value;
    }

    /**
     * Sets the value of PostingNarrative (1).
     * @param value The new value.
     **/
    public void setPostingNarrative(java.lang.String value)
    {
        this.postingNarrative = value;
    }

    /**
     * Sets the value of TxAmount (1).
     * @param value The new value.
     **/
    public void setTxAmount(biz.c24.io.training.statements.CurrencyAndAmount value)
    {
        this.txAmount = value;
        if (this.txAmount != null)
            ((biz.c24.io.api.data.ComplexDataObject) this.txAmount).setParent(this, "TxAmount");
    }

    /**
     * Sets the value of ValueDate (1).
     * @param value The new value.
     **/
    public void setValueDate(biz.c24.io.api.data.ISO8601Date value)
    {
        this.valueDate = value;
    }

    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException
    {
        out.writeObject(this.postingDate);
        out.writeObject(this.valueDate);
        out.writeObject(this.drCr);
        out.writeObject(this.txAmount);
        out.writeObject(this.postingNarrative);
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, java.lang.ClassNotFoundException
    {
        this.postingDate = (biz.c24.io.api.data.ISO8601Date) in.readObject();
        this.valueDate = (biz.c24.io.api.data.ISO8601Date) in.readObject();
        this.drCr = (java.lang.String) in.readObject();
        this.txAmount = (biz.c24.io.training.statements.CurrencyAndAmount) in.readObject();
        this.postingNarrative = (java.lang.String) in.readObject();
    }

}
