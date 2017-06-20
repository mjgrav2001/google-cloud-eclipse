
package com.google.cloud.tools.eclipse.appengine.facets;

import com.google.common.collect.Ordering;
import java.util.Comparator;

/**
 * A {@link Comparator<String>} for the Faceted Project framework that compares versions expressed
 * as strings.
 */
public class StringVersionComparator implements Comparator<String> {

  // Since this comparator is usually specified via plugin.xml, one would normally
  // implement this as an IExecutableExtensionFactory that returns Ordering.natural().
  // But the fproj framework instantiates the specified class *directly* and so we
  // must actually implement the comparator.

  @Override
  public int compare(String o1, String o2) {
    return Ordering.natural().compare(o1, o2);
  }

}
