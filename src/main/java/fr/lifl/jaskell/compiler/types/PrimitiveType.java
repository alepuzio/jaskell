/**
 *  Copyright Murex S.A.S., 2003-2013. All Rights Reserved.
 * 
 *  This software program is proprietary and confidential to Murex S.A.S and its affiliates ("Murex") and, without limiting the generality of the foregoing reservation of rights, shall not be accessed, used, reproduced or distributed without the
 *  express prior written consent of Murex and subject to the applicable Murex licensing terms. Any modification or removal of this copyright notice is expressly prohibited.
 */
package fr.lifl.jaskell.compiler.types;

import java.util.*;

import fr.lifl.jaskell.compiler.core.Primitives;


/**
 * @author  nono
 * @version $Id: PrimitiveType.java 1154 2005-11-24 21:43:37Z nono $
 */
public class PrimitiveType extends TypeConstructor implements Primitives {

    //~ ----------------------------------------------------------------------------------------------------------------
    //~ Static fields/initializers 
    //~ ----------------------------------------------------------------------------------------------------------------

    public static Map primitives = new HashMap();

    //~ ----------------------------------------------------------------------------------------------------------------
    //~ Instance fields 
    //~ ----------------------------------------------------------------------------------------------------------------

    /** java class equivalence for this type - may benull */
    private Class javaClass;

    //~ ----------------------------------------------------------------------------------------------------------------
    //~ Constructors 
    //~ ----------------------------------------------------------------------------------------------------------------

    /**
     * Constructor for PrimitiveType.
     *
     * @param name
     * @param type
     */
    public PrimitiveType(String name, Class clas) {
        this(name, clas, SimpleKind.K, null, null);
    }

    /**
     * Constructor for PrimitiveType.
     *
     * @param name
     * @param type
     */
    public PrimitiveType(String name, Class clas, Kind kind) {
        this(name, clas, kind, null, null);
    }

    /**
     * Constructor for PrimitiveType.
     *
     * @param name
     * @param type
     */
    public PrimitiveType(String name, Class clas, Kind kind, TypeApplicationFormat format, TypeComparator compar) {
        super(name, kind);
        this.javaClass = clas;
        if (format != null)
            setApplyFormatter(format);
        /* store generated object in table */
        primitives.put(name, this);
        /* store comparator */
        if (compar != null)
            Type.setComparatorFor(this, compar);
    }

    //~ ----------------------------------------------------------------------------------------------------------------
    //~ Methods 
    //~ ----------------------------------------------------------------------------------------------------------------

    /**
     * returns an iterator over a function type
     *
     * @param  type a type
     *
     * @return an instance of Iterator for traversing components of the function or null if type is not afunction
     */
    public static Iterator functionIterator(Type type) {
        if (!checkFunction(type))
            return null;
        return new FunctionTypeIterator(type);
    }

    /**
     * Method checkFunction.
     *
     * @param ft
     */
    public static boolean checkFunction(Type ft) {
        if (!ft.getConstructor().equals(Primitives.FUNCTION) || !ft.getKind().equals(SimpleKind.K))
            return false;
        return true;
    }

    /**
     * retrieve the number of arguments this function expects
     *
     * @param type the type to analyze which must be a function application
     */
    public static int getArgsCount(Type type) {
        if (!checkFunction(type))
            throw new TypeError("Cannot calculate argument count on non function type");
        int i = 0;
        while (checkFunction(type)) {
            type = ((TypeApplication) type).getRange();
            i++;
        }
        return i;
    }

    /**
     * A method to create simply function types This factory method is used to create function types from an domain and
     * range types
     *
     * @param dom   the domain of function
     * @param range the range of function
     */
    public static Type makeFunction(Type domain, Type range) {
        return TypeFactory.makeApplication(TypeFactory.makeApplication(FUNCTION, domain), range);
    }

    /**
     * Create a type constructor for given tuple size
     *
     * @param  n the size of the tuple type to create
     *
     * @return a type constructor for this tuple size
     */
    public static TypeConstructor makeTuple(int n) {
        if (n < 2)
            throw new TypeError("Cannot make tuples with less than two elements");
        StringBuffer sb = new StringBuffer("Prelude.((");
        Kind k = SimpleKind.K;
        for (int i = 0; i < n; i++) {
            k = new FunctionKind(SimpleKind.K, k);
            if (i < (n - 1))
                sb.append(',');
        }
        sb.append("))");
        return (TypeConstructor) TypeFactory.makeTycon(sb.toString(), k);
    }

    /**
     * Creates a function type from a list of arguments and a return type
     *
     * @param args a list of type arguments
     * @param ret  type of return
     */
    public static Type makeFunction(List args, Type ret) {
        Iterator it = args.iterator();
        Type t = ret;
        for (int i = args.size() - 1; i >= 0; i--)
            t = makeFunction((Type) args.get(i), t);
        return t;
    }

    /**
     * Method getReturnType.
     *
     * @param  uni
     *
     * @return Type
     */
    public static Type getReturnType(Type uni) {
        Type t = uni;
        while (checkFunction(t))
            t = ((TypeApplication) t).getRange();
        return t;
    }

    /**
     * @param  el
     *
     * @return
     */
    public static Type makeList(Type el) {
        return TypeFactory.makeApplication(LIST, el);
    }

    /**
     * @see jaskell.compiler.core.Type#visit(TypeVisitor)
     */
    public Object visit(TypeVisitor v) {
        return v.visit(this);
    }

    /**
     * Returns the javaClass.
     *
     * @return Class
     */
    public Class getJavaClass() {
        return javaClass;
    }

    /**
     * @see java.lang.Object#equals(Object)
     */
    public boolean equals(Object obj) {
        return super.equals(obj) && ((PrimitiveType) obj).javaClass.equals(javaClass);
    }

    /* (non-Javadoc)
     * @see jaskell.compiler.types.Type#setKind(jaskell.compiler.types.Kind)
     */
    public void setKind(Kind k) {
        throw new TypeError("Cannot set the kind of primitive type constructors");
    }

    //~ ----------------------------------------------------------------------------------------------------------------
    //~ Inner Classes 
    //~ ----------------------------------------------------------------------------------------------------------------

    /**
     * An iterator running over a FunctionType This iterator is used to iterate over domains and ranges of Function
     * types
     *
     * @author  bailly
     * @version $Id: PrimitiveType.java 1154 2005-11-24 21:43:37Z nono $
     */
    static class FunctionTypeIterator implements Iterator {

        /** base type */
        private Type function;

        /** next type */
        private Type curFunction;

        /**
         * Constructor for FunctionTypeIterator.
         */
        FunctionTypeIterator(Type ft) {
            /* check ft is a function type */
            if (!checkFunction(ft))
                throw new TypeError("Cannot iterate over non functional type");
            this.function = this.curFunction = ft;
        }

        /**
         * @see java.util.Iterator#hasNext()
         */
        public boolean hasNext() {
            return checkFunction(curFunction);
        }

        /**
         * @see java.util.Iterator#next()
         */
        public Object next() {
            if (!checkFunction(curFunction))
                throw new NoSuchElementException("No more function types in " + function);
            TypeApplication tmp = (TypeApplication) curFunction;
            Type dom = tmp.getDomain();
            Type rge = this.curFunction = tmp.getRange();
            Type niou = TypeFactory.makeApplication(dom, rge);
            return niou;
        }

        /**
         * @see java.util.Iterator#remove()
         */
        public void remove() {
            throw new UnsupportedOperationException("Cannot remove type from function");
        }

    }

}
