package VerletPhysics_Chain;




import java.util.ArrayList;
import java.util.Arrays;

import com.thomasdiewald.pixelflow.java.PixelFlow;
import com.thomasdiewald.pixelflow.java.verletphysics.SpringConstraint;
import com.thomasdiewald.pixelflow.java.verletphysics.VerletParticle2D;
import com.thomasdiewald.pixelflow.java.verletphysics.VerletPhysics2D;
import processing.core.*;

public class VerletPhysics_Chain extends PApplet {

  int viewport_w = 1280;
  int viewport_h = 720;
  int viewport_x = 230;
  int viewport_y = 0;
  
  // physics simulation
  VerletPhysics2D physics;
 
  // particle behavior, different presets for different bodies
  VerletParticle2D.Param param_chain = new VerletParticle2D.Param();
  
  // all we need is an array of particles
  int particles_count = 0;
  VerletParticle2D[] particles = new VerletParticle2D[particles_count];

  boolean DISPLAY_PARTICLES = true;

  // just for the window title-info
  int NUM_SPRINGS;
  int NUM_PARTICLES;
  
  
  public void settings(){
    size(viewport_w, viewport_h, P2D); 
    smooth(8);
  }
  


  public void setup() {
    surface.setLocation(viewport_x, viewport_y);
    
    // main library context
    PixelFlow context = new PixelFlow(this);
    context.print();
//    context.printGL();
    
    physics = new VerletPhysics2D();

    physics.param.GRAVITY = new float[]{ 0, 0.2f };
    physics.param.bounds  = new float[]{ 0, 0, width, height };
    physics.param.iterations_collisions = 4;
    physics.param.iterations_springs    = 4;
    
    // parameters for chain-particles
    param_chain.DAMP_BOUNDS          = 0.50f;
    param_chain.DAMP_COLLISION       = 0.9990f;
    param_chain.DAMP_VELOCITY        = 0.991f; 
    param_chain.DAMP_SPRING_decrease = 0.999999f; // contraction (... to restlength)
    param_chain.DAMP_SPRING_increase = 0.999999f; // expansion   (... to restlength)
    
    // create 200 particles at start
    for(int i = 0; i < 200; i++){
      float spawn_x = width/2 + random(-200, 200);
      float spawn_y = height/2 + random(-200, 200);
      createParticle(spawn_x, spawn_y);
    }

    frameRate(60);
  }
  
  
  
  public void reset(){
    particles_count = 0;
    particles = new VerletParticle2D[particles_count];
  }
  
  // creates a new particle, and links it with the previous one
  public void createParticle(float spawn_x, float spawn_y){
    // just in case, to avoid position conflicts
    spawn_x += random(-0.01f, +0.01f);
    spawn_y += random(-0.01f, +0.01f);
    
    int   idx_curr = particles_count;
    int   idx_prev = idx_curr - 1;
    float radius_collision_scale = 1.1f;
    float radius   = 5; 
    float rest_len = radius * 3 * radius_collision_scale;
    
    VerletParticle2D pa = new VerletParticle2D(idx_curr);
    pa.setMass(1);
    pa.setParamByRef(param_chain);
    pa.setPosition(spawn_x, spawn_y);
    pa.setRadius(radius);
    pa.setRadiusCollision(radius * radius_collision_scale);
    pa.setCollisionGroup(idx_curr); // every particle has a different collision-ID
    addParticleToList(pa);
    NUM_PARTICLES++;
    
    if(idx_prev >= 0){
      VerletParticle2D pb = particles[idx_prev];
      pa.px = pb.cx;
      pa.py = pb.cy;
      SpringConstraint.addSpring(pb, pa, rest_len*rest_len);
      NUM_SPRINGS++;
    }
  }
  
  
  // kind of the same what an ArrayList<VerletParticle2D> would do.
  public void addParticleToList(VerletParticle2D particle){
    if(particles_count >= particles.length){
      int new_len = (int) Math.max(2, Math.ceil(particles_count*1.5f) );
      if(particles == null){
        particles = new VerletParticle2D[new_len];
      } else {
        particles = Arrays.copyOf(particles, new_len);
      }
    }
    particles[particles_count++] = particle;
    physics.setParticles(particles, particles_count);
  }
  
  
  public void draw() {

    if(keyPressed && key == ' '){
      createParticle(mouseX, mouseY);
    }
    
    updateMouseInteractions();    
    
    // update physics simulation
    physics.update(1);
    
        
    // render
    background(255);
    
    noFill();
    strokeWeight(1);
    beginShape(LINES);
    for(int i = 0; i < particles_count; i++){
      VerletParticle2D pa = particles[i];
      for(int j = 0; j < pa.spring_count; j++){
        SpringConstraint spring = pa.springs[j];
        if(spring.is_the_good_one){
          VerletParticle2D pb = spring.pb;
          float force = Math.abs(spring.force);
          float r = force*5000f;
          float g = r/10;
          float b = 0;
          stroke(r,g,b);
          vertex(pa.cx, pa.cy);
          vertex(pb.cx, pb.cy);
        }
      }
    }
    endShape();
    
    if(DISPLAY_PARTICLES){
      noStroke();
      fill(0);
      for(int i = 0; i < particles_count; i++){
        VerletParticle2D particle = particles[i];
        ellipse(particle.cx, particle.cy, particle.rad*2, particle.rad*2);
      }
    }
    
    // interaction stuff
    if(DELETE_SPRINGS){
      fill(255,64);
      stroke(0);
      strokeWeight(1);
      ellipse(mouseX, mouseY, DELETE_RADIUS*2, DELETE_RADIUS*2);
    }

    // stats, to the title window
    String txt_fps = String.format(getClass().getName()+ "   [particles %d]   [springs %d]   [frame %d]   [fps %6.2f]", NUM_PARTICLES, NUM_SPRINGS, frameCount, frameRate);
    surface.setTitle(txt_fps);
  }
  

  
  //////////////////////////////////////////////////////////////////////////////
  // User Interaction
  //////////////////////////////////////////////////////////////////////////////
 
  VerletParticle2D particle_mouse = null;
  
  public VerletParticle2D findNearestParticle(float mx, float my){
    return findNearestParticle(mx, my, Float.MAX_VALUE);
  }
  
  public VerletParticle2D findNearestParticle(float mx, float my, float search_radius){
    float dd_min_sq = search_radius * search_radius;
    VerletParticle2D particle = null;
    for(int i = 0; i < particles_count; i++){
      float dx = mx - particles[i].cx;
      float dy = my - particles[i].cy;
      float dd_sq =  dx*dx + dy*dy;
      if( dd_sq < dd_min_sq){
        dd_min_sq = dd_sq;
        particle = particles[i];
      }
    }
    return particle;
  }
  
  public ArrayList<VerletParticle2D> findParticlesWithinRadius(float mx, float my, float search_radius){
    float dd_min_sq = search_radius * search_radius;
    ArrayList<VerletParticle2D> list = new ArrayList<VerletParticle2D>();
    for(int i = 0; i < particles_count; i++){
      float dx = mx - particles[i].cx;
      float dy = my - particles[i].cy;
      float dd_sq =  dx*dx + dy*dy;
      if(dd_sq < dd_min_sq){
        list.add(particles[i]);
      }
    }
    return list;
  }
  
  
  public void updateMouseInteractions(){
    // deleting springs/constraints between particles
    if(DELETE_SPRINGS){
      ArrayList<VerletParticle2D> list = findParticlesWithinRadius(mouseX, mouseY, DELETE_RADIUS);
      for(VerletParticle2D tmp : list){
        SpringConstraint.deactivateSprings(tmp);
        tmp.collision_group = physics.getNewCollisionGroupId();
        tmp.rad_collision = tmp.rad;
      }
    } else {
      if(particle_mouse != null) particle_mouse.moveTo(mouseX, mouseY, 0.2f);
    }
  }
  
  
  boolean DELETE_SPRINGS = false;
  float   DELETE_RADIUS  = 10;

  public void mousePressed(){
    if(mouseButton == RIGHT ) DELETE_SPRINGS = true;
    
    if(!DELETE_SPRINGS){
      particle_mouse = findNearestParticle(mouseX, mouseY, 100);
      if(particle_mouse != null) particle_mouse.enable(false, false, false);
    }
  }
  
  public void mouseReleased(){
    if(particle_mouse != null && !DELETE_SPRINGS){
      if(mouseButton == LEFT  ) particle_mouse.enable(true, true,  true );
      if(mouseButton == CENTER) particle_mouse.enable(true, false, false);
      particle_mouse = null;
    }
    if(mouseButton == RIGHT ) DELETE_SPRINGS = false;
  }
  
  public void keyReleased(){
    if(key == 'r') reset();
    if(key == 'p') DISPLAY_PARTICLES = !DISPLAY_PARTICLES;
  }

  public static void main(String args[]) {
    PApplet.main(new String[] { VerletPhysics_Chain.class.getName() });
  }
}