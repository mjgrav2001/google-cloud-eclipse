
package com.google.cloud.tools.eclipse.appengine.facets;

import java.util.Comparator;

/**
 * A {@link Comparator<String>} for the Faceted Project framework that compares versions that are
 * strings, like "JRE7" vs "JRE8", using the natural string ordering. Not intended for versions that
 * have some numeric meaning (e.g., 1.2.0): use the standard Faceted Project
 * {@link org.eclipse.wst.common.project.facet.core.DefaultVersionComparator}) comparator instead.
 */
public class StringVersionComparator implements Comparator<String> {

  // Since this comparator is usually specified via plugin.xml, one would normally
  // implement this as an IExecutableExtensionFactory that returns Ordering.natural().
  // But the fproj framework instantiates the specified class *directly* and so we
  // must actually implement the comparator.

  @Override
  public int compare(String o1, String o2) {
    return o1.compareTo(o2);
  }

}
