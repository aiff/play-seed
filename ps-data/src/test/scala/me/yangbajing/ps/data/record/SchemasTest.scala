package me.yangbajing.ps.data.record

import me.yangbajing.UnitPlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

/**
 * Schemas Test
 * Created by jingyang on 15/7/17.
 */
class SchemasTest extends UnitPlaySpec with GuiceOneAppPerSuite {

  "Schemas ddl" should {

    "drop tables" in {
      val schemas = app.injector.instanceOf[Schemas]
      import schemas.profile.api._
      val result = schemas.run(schemas.schema.drop.transactionally).futureValue
      result.mustBe(())
    }

    "create tables" in {
      val schemas = app.injector.instanceOf[Schemas]
      import schemas.profile.api._
      val result = schemas.run(schemas.schema.create.transactionally).futureValue
      result.mustBe(())
    }
  }

}
