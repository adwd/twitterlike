# JSONから50個の画像のうち一つをランダムで選び、画像、タイトル、リンクを表示する
getFlickrPhotos = (data) ->
  dataStat  = data.stat
  if(dataStat == 'ok')
    index = Math.floor( Math.random() * 50 )
    item = data.photos.photo[index]
    itemLink = "https://www.flickr.com/photos/#{item.owner}/#{item.id}"
    itemTitle = item.title
    itemTitle = "no title" if itemTitle == ""
    photoPath = "https://farm#{item.farm}.static.flickr.com/#{item.server}/#{item.id}_#{item.secret}_b.jpg"

    $("#flickr-box").css("visibility", "visible")
    $("#flickr-license").html('<a href="' + itemLink + '">' + itemTitle + '</a>')
    $.backstretch(photoPath, {fade: 1000})

$ ->
  # flickrから景色の画像で、ライセンスがCC BY 2.0のもののURLを50個得る
  $.ajax
    type : 'GET'
    url : 'https://www.flickr.com/services/rest/'
    data :
      format : 'json'
      method : 'flickr.photos.search'
      api_key : 'f19913083f594fe98f448414cbd25b84'
      sort : 'date-posted-desc'
      tags : 'landscape'
      per_page : '50'
      license: '4'
    dataType : 'jsonp'
    jsonp : 'jsoncallback'
    success : (data) -> getFlickrPhotos(data)
