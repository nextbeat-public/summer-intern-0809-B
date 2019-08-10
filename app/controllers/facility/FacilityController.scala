/*
 * This file is part of Nextbeat services.
 *
 * For the full copyright and license information,
 * please view the LICENSE file that was distributed with this source code.
 */

package controllers.facility

import play.api.i18n.I18nSupport
import play.api.mvc.{AbstractController, MessagesControllerComponents}
import persistence.facility.dao.FacilityDAO
import persistence.facility.model.Facility.formForFacilitySearch
import persistence.geo.model.Location
import persistence.geo.dao.LocationDAO
import model.site.facility.SiteViewValueFacilityList
import model.component.util.ViewValuePageLayout
import mvc.action.AuthenticationAction
import persistence.geo.model.Location.Region


// 施設
//~~~~~~~~~~~~~~~~~~~~~
class FacilityController @javax.inject.Inject()(
  val facilityDao: FacilityDAO,
  val daoLocation: LocationDAO,
  cc: MessagesControllerComponents
) extends AbstractController(cc) with I18nSupport {
  implicit lazy val executionContext = defaultExecutionContext

  /**
   * 施設詳細ページサンプル
   */
  def show() = Action.async { implicit request =>
    for {
      facility <- facilityDao.get(1)
    } yield {
      val vv = ViewValuePageLayout(id = request.uri)
      Ok(views.html.site.facility.show.Main(vv, facility.get))
    }
  }

  /**
    * 施設一覧ページ
    */
  def list = Action async { implicit request =>
    for {
      facilitySeq <- facilityDao.findAll
    } yield {
      val vv = SiteViewValueFacilityList(
        layout     = ViewValuePageLayout(id = request.uri),
        regions    = Region.map.map(_._1),
        facilities = facilitySeq
      )
      Ok(views.html.site.facility.list.Main(vv, formForFacilitySearch))
    }
  }

  /**
   * 施設検索
   */
  def search = Action.async { implicit request =>
    formForFacilitySearch.bindFromRequest.fold(
      errors => {
       for {
          facilitySeq <- facilityDao.findAll
        } yield {
          val vv = SiteViewValueFacilityList(
            layout     = ViewValuePageLayout(id = request.uri),
            regions    = Region.map.map(_._1),
            facilities = facilitySeq
          )
          BadRequest(views.html.site.facility.list.Main(vv, errors))
        }
      },
      form   => {
        for {
          facilitySeq <- form.regionIdOpt match {
            case Some(id) =>
              val regionMap = Region.map.toMap[Region, Seq[Location.Id]]
              for {
                // FIXME: regionMap()ではkeyがなくてnullになるケースが考慮できてなくてヌルポが発生する可能性があります。
                // nullを考慮した実装に修正してください
                locations   <- daoLocation.filterByRegion(regionMap(Region(id)))
                facilitySeq <- facilityDao.filterByLocationIds(locations.map(_.id))
              } yield facilitySeq
            case None     => facilityDao.findAll
          }
        } yield {
          val vv = SiteViewValueFacilityList(
            layout     = ViewValuePageLayout(id = request.uri),
            regions    = Region.map.map(_._1),
            facilities = facilitySeq
          )
          Ok(views.html.site.facility.list.Main(vv, formForFacilitySearch.fill(form)))
        }
      }
    )
  }
}
