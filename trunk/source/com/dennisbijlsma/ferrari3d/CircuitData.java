//-----------------------------------------------------------------------------
// Ferrari3D
// CircuitData
// (c) 2009 Dennis Bijlsma, BSD license
//-----------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d;

import com.dennisbijlsma.core3d.ImmutableVector3D;
import com.dennisbijlsma.ferrari3d.util.CircuitPoint;

/**
 * Interface for classes that contain circuit information. The most obvious
 * implementation is the circuit itself, but other classes can implement this
 * interface to test without having to load the circuit.
 */
public interface CircuitData {

	/**
	 * Returns the name of the circuit.
	 */
	public String getCircuitName();
	
	/**
	 * Returns the circuit point at the specified index. If the index is invalid
	 * the first point will be returned.
	 */
	public CircuitPoint getPoint(int index);
	
	/**
	 * Returns the number of points in this circuit.
	 */
	public int getNumPoints();
	
	/**
	 * Returns the intermediate at the specified index, or {@code null} if none
	 * exists.
	 */
	public CircuitPoint getIntermediate(int index);
	
	/**
	 * Returns the circuit camera closest to the specified coordinate.
	 */
	public ImmutableVector3D getClosestCamera(ImmutableVector3D v);
	
	/**
	 * Returns the starting grid position at the specified index.
	 * @throws IndexOutOfBoundException if no grid position exists at that index.
	 */
	public ImmutableVector3D getStartingGridPosition(int index);
}
