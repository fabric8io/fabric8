package biz.c24.io.training.statements;

import java.util.Arrays;

/**
 * Trailer. <p/>
 * This object is composed of the following <i>element</i>:
 * <ul>
 * <li><b>EndBalance</b> of type {@link biz.c24.io.training.statements.CurrencyAndAmount} (1)</li>
 * </ul>
 * @author C24 Integration Objects;
 * @see biz.c24.io.training.statements.TrailerClass
 **/
public class Trailer extends biz.c24.io.api.data.ComplexDataObject 
{
    private static final java.lang.String[] NATIVE_ELEMENTS = new String[] {"EndBalance"};
    private biz.c24.io.training.statements.CurrencyAndAmount endBalance;

    /**
     * Constructs a new instance defined by the default element.
     **/
    public Trailer()
    {
        this(biz.c24.io.training.statements.TrailerClass.getInstance().getNullDefiningElementDecl());
    }

    /**
     * Constructs a new instance defined by the specified element.
     * @param definingElementDecl The element which defines the object.
     **/
    public Trailer(biz.c24.io.api.data.Element definingElementDecl)
    {
        super(definingElementDecl);
    }

    /**
     * Constructs a new instance defined by the specified element and type.
     * @param definingElementDecl The element which defines the object.
     * @param type The type which defines the object.
     **/
    public Trailer(biz.c24.io.api.data.Element definingElementDecl, biz.c24.io.api.data.ComplexDataType type)
    {
        super(definingElementDecl, type);
    }

    /**
     * Constructs a new instance cloned from the specified object.
     * @param clone The object to be cloned.
     **/
    public Trailer(biz.c24.io.training.statements.Trailer clone)
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
            case 0:
                setEndBalance((biz.c24.io.training.statements.CurrencyAndAmount) value);
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
        return new biz.c24.io.training.statements.Trailer(this);
    }

    /**
     * Creates and returns a deep clone of this object.
     * @return The new object.
     * @see #clone()
     **/
    public biz.c24.io.api.data.ComplexDataObject cloneDeep() throws java.lang.CloneNotSupportedException
    {
        biz.c24.io.api.data.ComplexDataObject obj = new biz.c24.io.training.statements.Trailer(this);
        cloneDeep(obj);
        return obj;
    }

    protected void cloneDeep(biz.c24.io.api.data.ComplexDataObject clone) throws java.lang.CloneNotSupportedException
    {
        biz.c24.io.training.statements.Trailer obj = (biz.c24.io.training.statements.Trailer) clone;
        obj.endBalance = (biz.c24.io.training.statements.CurrencyAndAmount) biz.c24.io.api.Utils.cloneDeep(this.endBalance, obj, "EndBalance");
    }

    /**
     * Creates, adds and returns a new EndBalance (1).
     * @return The new value.
     **/
    public biz.c24.io.training.statements.CurrencyAndAmount createEndBalance()
    {
        biz.c24.io.training.statements.CurrencyAndAmount obj = (biz.c24.io.training.statements.CurrencyAndAmount) getElementDecl("EndBalance").createObject();
        setEndBalance(obj);
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
     * Returns the element called <code>name</code> at <code>index</code>.<p>
     * The legal value(s) for <code>name</code> are: <b>endBalance</b>.
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
            case 0:
                return this.endBalance;
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
            case 0:
                return this.endBalance == null ? 0 : 1;
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
            case 0:
                return this.endBalance != null && this.endBalance.equals(element) ? 0 : -1;
            default:
                return super.getElementIndex(name, element);
        }
    }

    /**
     * Gets the value of EndBalance (1).
     * @return The value.
     **/
    public biz.c24.io.training.statements.CurrencyAndAmount getEndBalance()
    {
        return this.endBalance;
    }

    public int getTotalElementCount()
    {
        int count = 0;
        count += this.endBalance == null ? 0 : 1;
        return count;
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
            case 0:
                setEndBalance(null);
                return;
            default:
            super.removeElement(name, index);
        }
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
            case 0:
                setEndBalance((biz.c24.io.training.statements.CurrencyAndAmount) value);
                return;
            default:
                super.setElement(name, index, value);
        }
    }

    /**
     * Sets the value of EndBalance (1).
     * @param value The new value.
     **/
    public void setEndBalance(biz.c24.io.training.statements.CurrencyAndAmount value)
    {
        this.endBalance = value;
        if (this.endBalance != null)
            ((biz.c24.io.api.data.ComplexDataObject) this.endBalance).setParent(this, "EndBalance");
    }

    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException
    {
        out.writeObject(this.endBalance);
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, java.lang.ClassNotFoundException
    {
        this.endBalance = (biz.c24.io.training.statements.CurrencyAndAmount) in.readObject();
    }

}
