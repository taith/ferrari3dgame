//-----------------------------------------------------------------------------
// Ferrari3D
// TestGraphics
// (c) 2009 Dennis Bijlsma, BSD license
//-----------------------------------------------------------------------------

package com.dennisbijlsma.ferrari3d.test;

import com.dennisbijlsma.core3d.Display;
import com.dennisbijlsma.core3d.DisplayWindow;
import com.dennisbijlsma.core3d.GameCore;
import com.dennisbijlsma.core3d.Vector3D;
import com.dennisbijlsma.core3d.internal.SceneHelper;
import com.dennisbijlsma.core3d.scene.Camera;
import com.dennisbijlsma.core3d.scene.LightSource;
import com.dennisbijlsma.core3d.scene.Model;
import com.dennisbijlsma.core3d.scene.SceneGraphNode;
import nl.colorize.util.ResourceFile;

/**
 * Test class for car graphics.
 */

public class TestGraphics extends GameCore {
	
	public static void main(String[] args) {
		TestGraphics test = new TestGraphics();
		test.startGame();
	}

	public TestGraphics() {
		super();
	}
	
	@Override
	protected Display initDisplay() {
		return new DisplayWindow(640, 480, false);
	}

	@Override
	public void initGameState() {
		
		Camera camera = new Camera(getDisplay());
		getSceneGraph().addCamera(camera);
		camera.aim(new Vector3D(0f, 5f, -10f), new Vector3D());
		
		LightSource light = LightSource.createDirectionalLight(new Vector3D(0f, -1f, 0.5f));
		getSceneGraph().addLight(light);
		
		SceneGraphNode floor = SceneHelper.createCheckerboardFloor(10, 10);
		getSceneGraph().getRootNode().addChild(floor);

		ResourceFile objFile = new ResourceFile("cars/Ferrari 248/resources/ferrari.obj");
		Model obj = new Model("obj", objFile.getURL(), objFile.getURL());
		obj.getTransform().setScale(0.1f);
		obj.getTransform().setPosition(-2f, 0f, 0f);
		getSceneGraph().getRootNode().addChild(obj);

		ResourceFile jmeFile = new ResourceFile("cars/Ferrari 248/resources/ferrari.jme");
		Model jme = new Model("jme", jmeFile.getURL(), jmeFile.getURL());
		jme.getTransform().setScale(0.1f);
		jme.getTransform().setPosition(2f, 0f, 0f);
		getSceneGraph().getRootNode().addChild(jme);
	}

	@Override
	public void updateGameState(float dt) {
		getSceneGraph().getRootNode().getTransform().getRotation().y += 0.002f;
		SceneHelper.updateMouseLookControls(getController(), getSceneGraph().getCamera(0));
	}
}
