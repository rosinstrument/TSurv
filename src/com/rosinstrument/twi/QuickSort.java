package com.rosinstrument.twi;

import java.util.Vector;

/**
 *
 * @author mvlad
 */
public class QuickSort {

    public static Vector reverse(Vector v) {
        if (v != null && v.size() > 0) {
            int size = v.size() - 1;
            for (int i = 0; i < (size + 1) / 2; i++) {
                Object a = v.elementAt(i);
                int si = size - i;
                v.setElementAt(v.elementAt(si), i);
                v.setElementAt(a, si);
            }
        }
        return v;
    }

    public static String[] sort(String a[]) {
        return sort(a, 0, a.length - 1);
    }

    /**
     * This is a generic version of C.A.R Hoare's Quick Sort algorithm. This
     * will handle arrays that are already sorted, and arrays with duplicate
     * keys. <BR>
     *
     * If you think of a one dimensional array as going from the lowest index on
     * the left to the highest index on the right then the parameters to this
     * function are lowest index or left and highest index or right. The first
     * time you call this function it will be with the parameters 0, a.length -
     * 1.
     *
     * @param a a string array
     * @param lo0 left boundary of array partition
     * @param hi0 right boundary of array partition
     */
    static String[] sort(String a[], int lo0, int hi0) {
        int lo = lo0;
        int hi = hi0;
        String mid;

        if (hi0 > lo0) {

            /*
             * Arbitrarily establishing partition element as the midpoint of the
             * array.
             */
            mid = a[(lo0 + hi0) / 2];

            // loop through the array until indices cross
            while (lo <= hi) {
                /*
                 * find the first element that is greater than or equal to the
                 * partition element starting from the left Index.
                 */
                while ((lo < hi0) && (a[lo].compareTo(mid) < 0)) {
                    ++lo;
                }

                /*
                 * find an element that is smaller than or equal to the
                 * partition element starting from the right Index.
                 */
                while ((hi > lo0) && (a[hi].compareTo(mid) > 0)) {
                    --hi;
                }

                // if the indexes have not crossed, swap
                if (lo <= hi) {
                    String t = a[hi];
                    a[hi] = a[lo];
                    a[lo] = t;

                    ++lo;
                    --hi;
                }
            }

            /*
             * If the right index has not reached the left side of array must
             * now sort the left partition.
             */
            if (lo0 < hi) {
                sort(a, lo0, hi);
            }

            /*
             * If the left index has not reached the right side of array must
             * now sort the right partition.
             */
            if (lo < hi0) {
                sort(a, lo, hi0);
            }

        }
        return a;
    }

    public static Vector sort(Vector a) {
        if (a != null && !a.isEmpty()) {
            sort(a, 0, a.size() - 1);
        }
        return a;
    }

    static Vector sort(Vector a, int lo0, int hi0) {
        int lo = lo0;
        int hi = hi0;
        String mid;

        if (hi0 > lo0) {

            /*
             * Arbitrarily establishing partition element as the midpoint of the
             * array.
             */
            mid = (String) a.elementAt((lo0 + hi0) / 2);

            // loop through the array until indices cross
            while (lo <= hi) {
                /*
                 * find the first element that is greater than or equal to the
                 * partition element starting from the left Index.
                 */
                while ((lo < hi0) && (((String) a.elementAt(lo)).compareTo(mid) < 0)) {
                    ++lo;
                }

                /*
                 * find an element that is smaller than or equal to the
                 * partition element starting from the right Index.
                 */
                while ((hi > lo0) && (((String) a.elementAt(hi)).compareTo(mid) > 0)) {
                    --hi;
                }

                // if the indexes have not crossed, swap
                if (lo <= hi) {
                    String t = (String) a.elementAt(hi);
                    a.setElementAt(a.elementAt(lo), hi);
                    a.setElementAt(t, lo);

                    ++lo;
                    --hi;
                }
            }

            /*
             * If the right index has not reached the left side of array must
             * now sort the left partition.
             */
            if (lo0 < hi) {
                sort(a, lo0, hi);
            }

            /*
             * If the left index has not reached the right side of array must
             * now sort the right partition.
             */
            if (lo < hi0) {
                sort(a, lo, hi0);
            }

        }
        return a;
    }
    

    public static Vector sort(Vector a, Comparator comp) {
        if (a != null && !a.isEmpty()) {
            sort(a, 0, a.size() - 1, comp);
        }
        return a;
    }

    static Vector sort(Vector a, int lo0, int hi0, Comparator comp) {
        int lo = lo0;
        int hi = hi0;
        String mid;

        if (hi0 > lo0) {

            /*
             * Arbitrarily establishing partition element as the midpoint of the
             * array.
             */
            mid = (String) a.elementAt((lo0 + hi0) / 2);

            // loop through the array until indices cross
            while (lo <= hi) {
                /*
                 * find the first element that is greater than or equal to the
                 * partition element starting from the left Index.
                 */
                while ((lo < hi0) && comp.compareTo((String) a.elementAt(lo), mid) < 0) {
                    ++lo;
                }

                /*
                 * find an element that is smaller than or equal to the
                 * partition element starting from the right Index.
                 */
                while ((hi > lo0) && comp.compareTo((String) a.elementAt(hi), mid) > 0) {
                    --hi;
                }

                // if the indexes have not crossed, swap
                if (lo <= hi) {
                    String t = (String) a.elementAt(hi);
                    a.setElementAt(a.elementAt(lo), hi);
                    a.setElementAt(t, lo);

                    ++lo;
                    --hi;
                }
            }

            /*
             * If the right index has not reached the left side of array must
             * now sort the left partition.
             */
            if (lo0 < hi) {
                sort(a, lo0, hi);
            }

            /*
             * If the left index has not reached the right side of array must
             * now sort the right partition.
             */
            if (lo < hi0) {
                sort(a, lo, hi0);
            }

        }
        return a;
    }

    
}
