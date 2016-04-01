/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.qualcomm.vuforia.samples.VuforiaSamples.ui.ActivityList.libGDX;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight;
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader;
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.linearmath.LinearMath;
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw.DebugDrawModes;

import com.badlogic.gdx.Gdx.*;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.qualcomm.vuforia.samples.VuforiaSamples.ui.ActivityList.DataHolder;

/** @author xoppa */
public class BaseBulletTest extends BulletTest {
	// Set this to the path of the lib to use it on desktop instead of default lib.
	private final static String customDesktopLib = null;//"D:\\Xoppa\\code\\libgdx\\extensions\\gdx-bullet\\jni\\vs\\gdxBullet\\x64\\Debug\\gdxBullet.dll";

	private static boolean initialized = false;
	
	public static boolean shadows = true;
	
	public static void init () {
		if (initialized) return;
		// Need to initialize bullet before using it.
		if (Gdx.app.getType() == ApplicationType.Desktop && customDesktopLib != null) {
			System.load(customDesktopLib);
		} else
			Bullet.init();
		Gdx.app.log("Bullet", "Version = " + LinearMath.btGetVersion());
		initialized = true;
	}

	public Environment environment;
	public DirectionalLight light;
	public ModelBatch shadowBatch;

	public BulletWorld world;
	public ObjLoader objLoader = new ObjLoader();
	public ModelBuilder modelBuilder = new ModelBuilder();
	public ModelBatch modelBatch;
	public Array<Disposable> disposables = new Array<Disposable>();
	private int debugMode = DebugDrawModes.DBG_NoDebug;
	
	protected final static Vector3 tmpV1 = new Vector3(), tmpV2 = new Vector3(),  tmpV3 = new Vector3();

	public BulletWorld createWorld () {
		return new BulletWorld();
	}

	@Override
	public void create () {
		init();
		environment = new Environment();
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.3f, 0.3f, 0.3f, 1.f));
		light = shadows ? new DirectionalShadowLight(1024, 1024, 20f, 20f, 1f, 300f) : new DirectionalLight();
		light.set(0.8f, 0.8f, 0.8f, -0.5f, -1f, 0.7f);
		environment.add(light);
		if (shadows)
			environment.shadowMap = (DirectionalShadowLight)light;
		shadowBatch = new ModelBatch(new DepthShaderProvider());

		modelBatch = new ModelBatch();


		world = createWorld();
		world.performanceCounter = performanceCounter;


		final float width = Gdx.graphics.getWidth();
		final float height = Gdx.graphics.getHeight();

		camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		camera.position.set(5, 5, 5);
		camera.lookAt(0, 1, 0);
		camera.near = 0.1f;
		camera.far = 300f;
		camera.update();

		// Create some simple models
		final Model groundModel = modelBuilder.createRect(
			20f,
			0f,
			-20f,
			-20f,
			0f,
			-20f,
			-20f,
			0f,
			20f,
			20f,
			0f,
			20f,
			0,
			1,
			0,
			new Material(ColorAttribute.createDiffuse(Color.WHITE), ColorAttribute.createSpecular(Color.WHITE), FloatAttribute
				.createShininess(16f)), Usage.Position | Usage.Normal);
		disposables.add(groundModel);
		final Model boxModel = modelBuilder.createBox(1f, 1f, 1f, new Material(ColorAttribute.createDiffuse(Color.WHITE),
			ColorAttribute.createSpecular(Color.WHITE), FloatAttribute.createShininess(64f)), Usage.Position | Usage.Normal);
		disposables.add(boxModel);

		// Add the constructors
		world.addConstructor("ground", new BulletConstructor(groundModel, 0f)); // mass = 0: static body
		world.addConstructor("box", new BulletConstructor(boxModel, 1f)); // mass = 1kg: dynamic body
		world.addConstructor("staticbox", new BulletConstructor(boxModel, 0f)); // mass = 0: static body
	}

	@Override
	public void dispose () {
		world.dispose();
		world = null;

		for (Disposable disposable : disposables)
			disposable.dispose();
		disposables.clear();

		modelBatch.dispose();
		modelBatch = null;

		shadowBatch.dispose();
		shadowBatch = null;

		if (shadows)
			((DirectionalShadowLight)light).dispose();
		light = null;

		super.dispose();
	}

	@Override
	public void render () {
		render(true);
	}

	public void render (boolean update) {

		fpsCounter.put(Gdx.graphics.getFramesPerSecond());

		if (update) update();

		beginRender(true);

		renderWorld();

		Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
		if (debugMode != DebugDrawModes.DBG_NoDebug) world.setDebugMode(debugMode);
		Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);



		performance.setLength(0);
		performance.append("FPS: ").append(fpsCounter.value).append(", Bullet: ")
			.append((int)(performanceCounter.load.value * 100f)).append("%");
	}

	protected void beginRender (boolean lighting) {
		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		Gdx.gl.glClearColor(0, 0, 0, 0);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
//		camera.update();
	}

	protected void renderWorld () {
		if (shadows) {
			((DirectionalShadowLight)light).begin(Vector3.Zero, camera.direction);
			shadowBatch.begin(((DirectionalShadowLight)light).getCamera());
			world.render(shadowBatch, null);
			shadowBatch.end();
			((DirectionalShadowLight)light).end();
		}

		float[] modelViewMatrix = DataHolder.getInstance().getData();

		float[] modelProjMatrix = DataHolder.getInstance().getData2();

		if(modelViewMatrix != null && modelProjMatrix != null)
		{


			Matrix4 temp = new Matrix4(modelViewMatrix.clone());

			Matrix4 temp2 = new Matrix4(modelProjMatrix.clone());
//
//			temp = temp.inv();

//			camera.combined.set(temp);
//
//			Matrix4 temp3 = camera.view;
//
//			float[] view = temp3.getValues();


			float[] viewMatrix = temp.getValues();

			float[] projMatrix = temp2.getValues();




			float posZ = viewMatrix[14];
			float posY = viewMatrix[13];
			float posX = viewMatrix[12];

			float dirZ = viewMatrix[10];
			float dirY = viewMatrix[9];
			float dirX = viewMatrix[8];

			float upZ = viewMatrix[6];
			float upY = viewMatrix[5];
			float upX = viewMatrix[4];


			camera.position.set(posX, posY, posZ);
			camera.lookAt(dirX, dirY, dirZ);
			camera.up.set(upX, upY, upZ);



			camera.update();



//                Gdx.app.log("---------------------------------------------","------------------------------------------------------------------");
//                Gdx.app.log("aa", "" + modelViewMatrix[0] + "  " + modelViewMatrix[1] + "  " + modelViewMatrix[2] + "  " + modelViewMatrix[3]);
//                Gdx.app.log("aa", "" + modelViewMatrix[4] + "  " + modelViewMatrix[5] + "  " + modelViewMatrix[6] + "  " + modelViewMatrix[7]);
//                Gdx.app.log("a", "" + modelViewMatrix[8] + "  " + modelViewMatrix[9] + "  " + modelViewMatrix[10] + "  " + modelViewMatrix[11]);
//                Gdx.app.log("aa", "" + modelViewMatrix[12] + "  " + modelViewMatrix[13] + "  " + modelViewMatrix[14] + "  " + modelViewMatrix[15]);
//                Gdx.app.log("---------------------------------------------", "------------------------------------------------------------------");
//
//                camera.view.mul(temp);

		}

//		camera.update();



		modelBatch.begin(camera);
		world.render(modelBatch, environment);
		modelBatch.end();
	}

	public void update () {
		world.update();
	}

	public BulletEntity shoot (final float x, final float y) {
		return shoot(x, y, 30f);
	}

	public BulletEntity shoot (final float x, final float y, final float impulse) {
		return shoot("box", x, y, impulse);
	}

	public BulletEntity shoot (final String what, final float x, final float y, final float impulse) {
		// Shoot a box
		Ray ray = camera.getPickRay(x, y);
		BulletEntity entity = world.add(what, ray.origin.x, ray.origin.y, ray.origin.z);
		entity.setColor(0.5f + 0.5f * (float)Math.random(), 0.5f + 0.5f * (float)Math.random(), 0.5f + 0.5f * (float)Math.random(),
			1f);
		((btRigidBody)entity.body).applyCentralImpulse(ray.direction.scl(impulse));
		return entity;
	}

	public void setDebugMode (final int mode) {
		world.setDebugMode(debugMode = mode);
	}

	public void toggleDebugMode () {
		if (world.getDebugMode() == DebugDrawModes.DBG_NoDebug)
			setDebugMode(DebugDrawModes.DBG_DrawWireframe | DebugDrawModes.DBG_DrawFeaturesText | DebugDrawModes.DBG_DrawText | DebugDrawModes.DBG_DrawContactPoints);
		else if (world.renderMeshes)
			world.renderMeshes = false;
		else {
			world.renderMeshes = true;
			setDebugMode(DebugDrawModes.DBG_NoDebug);
		}
	}

	@Override
	public boolean longPress (float x, float y) {
		toggleDebugMode();
		return true;
	}

	@Override
	public boolean keyUp (int keycode) {
		if (keycode == Keys.ENTER) {
			toggleDebugMode();
			return true;
		}
		return super.keyUp(keycode);
	}
}