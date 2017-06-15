
package com.google.cloud.tools.eclipse.appengine.facets;

import com.google.common.collect.Ordering;
import java.util.Comparator;

/**
 * A {@link Comparator<String>} for the Faceted Project framework that compares versions expressed
 * as strings.
 */
public class StringVersionComparator implements Comparator<String> {

  // Although normally specified in a <code>plugin.xml</code>, the fproj framework
  // instantiates the specified class directly and so we cannot just implement
  // IExecutableExtensionFactory that returns Ordering.natural().

  @Override
  public int compare(String o1, String o2) {
    return Ordering.natural().compare(o1, o2);
  }

}
