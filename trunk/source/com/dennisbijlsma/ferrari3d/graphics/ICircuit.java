//--------------------------------------------------------------------------------
// Ferrari3D
// ICircuit
//--------------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.graphics;

import com.dennisbijlsma.ferrari3d.util.*;

/**
 * Interface for circuits. This mainly exists for testing purposes, to link non-3D
 * graphics to the <code>Contestant</code> class. Normally the default 
 * implementation of <code>Circuit</code> should be used.
 */

public interface ICircuit {

	/**
	 * Returns the circuit point at the specified index. When no point is 
	 * available at this index, the first point will be returned.
	 */
	
	public CircuitPoint getPoint(int index);
	
	/**
	 * Returns all circuit points as an array.
	 */
	
	public CircuitPoint[] getPoints();
	
	/**
	 * Returns the intermediate at the specified index. When no intermediate is
	 * available this method will return <code>null</code>.
	 */
	
	public CircuitPoint getIntermediate(int index);
	
	/**
	 * Returns the circuit's name.
	 */
	
	public String getCircuitName();
}