package com.pawhunt.app.preview

import kotlin.math.*

/** Fixed pool in world space. Called once after each 120 Hz locomotion step. */
class BugTrail(private val config: Config) {
    data class Style(val lifetime: Pair<Double, Double>, val size: Pair<Double, Double>,
                     val spin: Pair<Double, Double>, val opacity: Pair<Double, Double>,
                     val drift: Double, val rise: Double, val inheritVelocity: Double,
                     val endScale: Double, val palette: List<DoubleArray>, val rateMultiplier: Double = 1.0)
    data class Config(val style: String, val seed: Long, val maxParticles: Int,
                      val tailOffset: Double, val spread: Double, val minSpeed: Double,
                      val fullEmissionSpeed: Double, val maxSpeed: Double,
                      val rate: Pair<Double, Double>, val styles: Map<String, Style>)
    class Particle {
        var active=false; var alpha=0.0; var style="stars"; var id=0
        var age=0.0; var life=1.0; var baseSize=0.0; var startAngle=0.0
        var spin=0.0; var opacity=0.0; var color=DoubleArray(3)
        var ox=0.0; var oy=0.0; var oz=0.0; var vx=0.0; var vz=0.0
        var rise=0.0; var endScale=1.0
        var x=0.0; var y=0.0; var z=0.0; var angle=0.0; var size=0.0
    }
    companion object {
        private const val STEP=1.0/120
        private fun mix(a: Double,b: Double,t: Double)=a+(b-a)*t
        private fun smooth(value: Double): Double { val x=value.coerceIn(0.0,1.0);return x*x*(3-2*x) }
        private fun linear(value: Double): Double {
            val v=value/255
            return if(v<=.04045) v/12.92 else ((v+.055)/1.055).pow(2.4)
        }
    }
    var style=config.style; private set
    val particles=List(config.maxParticles) { Particle() }
    var spawned=0; private set
    private var randomState=config.seed
    private var credit=0.0; private var nextEmission=1.0
    private val colors=config.styles.mapValues { (_,s)->s.palette.map { c->DoubleArray(3) { linear(c[it]) } } }
    private fun random(): Double { randomState=randomState*48271%2147483647;return randomState.toDouble()/2147483647 }
    private fun range(pair: Pair<Double,Double>)=mix(pair.first,pair.second,random())

    fun select(value: String) {
        require(value=="none" || config.styles.containsKey(value)) { "Unknown trail: $value" }
        style=value;credit=0.0;nextEmission=1.0
        particles.forEach { it.active=false;it.alpha=0.0 }
    }
    fun step(pose: BugMotion.Pose) {
        for(p in particles) {
            if(!p.active)continue
            p.age+=STEP
            if(p.age>=p.life) { p.active=false;p.alpha=0.0;continue }
            update(p)
        }
        if(style=="none")return
        val c=config;val speed=pose.speed
        val gate=smooth((speed-c.minSpeed)/(c.fullEmissionSpeed-c.minSpeed))
        if(gate==0.0) { credit=0.0;return }
        credit+=mix(c.rate.first,c.rate.second,(speed/c.maxSpeed).coerceIn(0.0,1.0))*c.styles.getValue(style).rateMultiplier*gate*STEP
        if(credit>=nextEmission) {
            credit-=nextEmission;spawn(pose);nextEmission=.78+random()*.44
        }
    }
    private fun spawn(pose: BugMotion.Pose) {
        val p=particles.firstOrNull { !it.active } ?: return
        val c=config;val s=c.styles.getValue(style);val a=pose.yaw
        val side=(random()*2-1)*c.spread;val behind=c.tailOffset*pose.scaleZ
        p.style=style;p.age=0.0;p.life=range(s.lifetime);p.baseSize=range(s.size)
        p.startAngle=random()*PI*2;p.spin=range(s.spin);p.opacity=range(s.opacity)
        val palette=colors.getValue(style);p.color=palette[floor(random()*palette.size).toInt()]
        p.ox=pose.x+sin(a)*behind+cos(a)*side
        p.oz=pose.z+cos(a)*behind-sin(a)*side;p.oy=pose.height+.055
        val drift=(random()*2-1)*s.drift
        p.vx=-sin(a)*pose.speed*s.inheritVelocity+cos(a)*drift
        p.vz=-cos(a)*pose.speed*s.inheritVelocity-sin(a)*drift
        p.rise=s.rise;p.endScale=s.endScale;p.active=true;p.id=++spawned;update(p)
    }
    private fun update(p: Particle) {
        val t=p.age/p.life
        p.x=p.ox+p.vx*p.age;p.z=p.oz+p.vz*p.age;p.y=p.oy+p.rise*p.age
        p.angle=p.startAngle+p.spin*p.age
        p.size=p.baseSize*mix(.76,1.0,smooth(t/.14))*mix(1.0,p.endScale,smooth(t))
        p.alpha=p.opacity*smooth(p.age/.025)*(1-smooth((t-.18)/.82))
    }
}
