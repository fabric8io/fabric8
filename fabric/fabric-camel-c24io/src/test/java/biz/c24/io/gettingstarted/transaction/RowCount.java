package biz.c24.io.gettingstarted.transaction;

import java.util.Arrays;

/**
 * Row Count. <p/>
 * This object is composed of the following <i>elements</i>:
 * <ul>
 * <li><b>Prefix</b> of type {@link java.lang.String} (1)</li>
 * <li><b>Value</b> of type <code>long</code> (0..1)</li>
 * </ul>
 * @author C24 Integration Objects;
 * @see biz.c24.io.gettingstarted.transaction.RowCountClass
 **/
public class RowCount extends biz.c24.io.api.data.ComplexDataObject 
{
    private static final java.lang.String[] NATIVE_ELEMENTS = new String[] {"Prefix", "Value"};
    private java.lang.String prefix;
    private long value;
    private boolean isvalueSet;

    /**
     * Constructs a new instance defined by the default element.
     **/
    public RowCount()
    {
        this(biz.c24.io.gettingstarted.transaction.RowCountClass.getInstance().getNullDefiningElementDecl());
    }

    /**
     * Constructs a new instance defined by the specified element.
     * @param definingElementDecl The element which defines the object.
     **/
    public RowCount(biz.c24.io.api.data.Element definingElementDecl)
    {
        super(definingElementDecl);
    }

    /**
     * Constructs a new instance defined by the specified element and type.
     * @param definingElementDecl The element which defines the object.
     * @param type The type which defines the object.
     **/
    public RowCount(biz.c24.io.api.data.Element definingElementDecl, biz.c24.io.api.data.ComplexDataType type)
    {
        super(definingElementDecl, type);
    }

    /**
     * Constructs a new instance cloned from the specified object.
     * @param clone The object to be cloned.
     **/
    public RowCount(biz.c24.io.gettingstarted.transaction.RowCount clone)
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
                setPrefix((java.lang.String) value);
                return;
            case 1:
                setValue(biz.c24.io.api.Utils.longValue(value));
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
        return new biz.c24.io.gettingstarted.transaction.RowCount(this);
    }

    /**
     * Creates and returns a deep clone of this object.
     * @return The new object.
     * @see #clone()
     **/
    public biz.c24.io.api.data.ComplexDataObject cloneDeep() throws java.lang.CloneNotSupportedException
    {
        biz.c24.io.api.data.ComplexDataObject obj = new biz.c24.io.gettingstarted.transaction.RowCount(this);
        cloneDeep(obj);
        return obj;
    }

    protected void cloneDeep(biz.c24.io.api.data.ComplexDataObject clone) throws java.lang.CloneNotSupportedException
    {
        biz.c24.io.gettingstarted.transaction.RowCount obj = (biz.c24.io.gettingstarted.transaction.RowCount) clone;
        obj.prefix = (java.lang.String) biz.c24.io.api.Utils.cloneDeep(this.prefix, obj, "Prefix");
        obj.isvalueSet = this.isvalueSet;
        obj.value = this.value;
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
     * The legal value(s) for <code>name</code> are: <b>prefix, value</b>.
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
                return this.prefix;
            case 1:
                return new java.lang.Long((long) this.value);
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
                return this.prefix == null ? 0 : 1;
            case 1:
                return this.isvalueSet ? 1 : 0;
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
                return this.prefix != null && this.prefix.equals(element) ? 0 : -1;
            case 1:
                return this.isvalueSet ? (this.value == biz.c24.io.api.Utils.longValue(element) ? 0 : -1) : -1;
            default:
                return super.getElementIndex(name, element);
        }
    }

    /**
     * Gets the value of Prefix (1).
     * @return The value.
     **/
    public java.lang.String getPrefix()
    {
        return this.prefix;
    }

    public int getTotalElementCount()
    {
        int count = 0;
        count += this.prefix == null ? 0 : 1;
        count += this.isvalueSet ? 1 : 0;
        return count;
    }

    /**
     * Gets the value of Value (0..1).
     * @return The value.
     **/
    public long getValue()
    {
        return this.value;
    }

    public int hashCode()
    {
        return this.toString().length();
    }

    /**
     * Tests whether Value has been set.
     **/
    public boolean isValueSet()
    {
        return isvalueSet;
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
                setPrefix(null);
                return;
            case 1:
                setValue(0);
                this.isvalueSet = false;
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
                setPrefix((java.lang.String) value);
                return;
            case 1:
                setValue(biz.c24.io.api.Utils.longValue(value));
                return;
            default:
                super.setElement(name, index, value);
        }
    }

    /**
     * Sets the value of Prefix (1).
     * @param value The new value.
     **/
    public void setPrefix(java.lang.String value)
    {
        this.prefix = value;
    }

    /**
     * Sets the value of Value (0..1).
     * @param value The new value.
     **/
    public void setValue(long value)
    {
        this.value = value;
        this.isvalueSet = true;
    }

    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException
    {
        out.writeObject(this.prefix);
        out.writeBoolean(this.isvalueSet);
        if (this.isvalueSet)
            out.writeLong(this.value);
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, java.lang.ClassNotFoundException
    {
        this.prefix = (java.lang.String) in.readObject();
        this.isvalueSet = in.readBoolean();
        if (this.isvalueSet)
            this.value = in.readLong();
    }

}
