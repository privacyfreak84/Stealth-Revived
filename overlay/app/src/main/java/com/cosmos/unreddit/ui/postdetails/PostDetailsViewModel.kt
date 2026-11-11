package com.cosmos.unreddit.ui.postdetails

import androidx.lifecycle.viewModelScope
import com.cosmos.unreddit.data.model.Comment
import com.cosmos.unreddit.data.model.Comment.CommentEntity
import com.cosmos.unreddit.data.model.Comment.MoreEntity
import com.cosmos.unreddit.data.model.Resource
import com.cosmos.unreddit.data.model.Sort
import com.cosmos.unreddit.data.model.Sorting
import com.cosmos.unreddit.data.model.db.PostEntity
import com.cosmos.unreddit.data.remote.api.reddit.model.MoreChildren
import com.cosmos.unreddit.data.repository.PostListRepository
import com.cosmos.unreddit.data.repository.PreferencesRepository
import com.cosmos.unreddit.di.DispatchersModule.DefaultDispatcher
import com.cosmos.unreddit.ui.base.BaseViewModel
import com.cosmos.unreddit.util.PostUtil
import com.cosmos.unreddit.util.extension.updateValue
import com.cosmos.unreddit.data.local.mapper.CommentMapper2
import com.cosmos.unreddit.data.local.mapper.PostMapper2
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

/** Comments as shown: the flattened visible tree, plus a revision for in-place changes. */
data class CommentsUi(val resource: Resource<List<Comment>>, val revision: Int = 0)

@HiltViewModel
class PostDetailsViewModel @Inject constructor(
    preferencesRepository: PreferencesRepository,
    private val repository: PostListRepository,
    private val postMapper: PostMapper2,
    private val commentMapper: CommentMapper2,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : BaseViewModel(preferencesRepository, repository) {

    private val _sorting = MutableStateFlow(DEFAULT_SORTING)
    val sorting: StateFlow<Sorting> = _sorting

    private val _permalink = MutableStateFlow<String?>(null)
    val permalink: StateFlow<String?> = _permalink

    private val _singleThread = MutableStateFlow(false)
    val singleThread: StateFlow<Boolean> = _singleThread

    val savedCommentIds: Flow<List<String>> = currentProfile.flatMapLatest {
        repository.getSavedCommentIds(it.id)
    }

    private val _post = MutableStateFlow<Resource<PostEntity>>(Resource.Loading())

    val post: Flow<Resource<PostEntity>> = combine(_post, savedPostIds) { post, savedIds ->
        post.apply { if (this is Resource.Success) data.saved = savedIds.contains(data.id) }
    }.flowOn(defaultDispatcher)

    private val _comments = MutableStateFlow(CommentsUi(Resource.Loading()))
    val comments: StateFlow<CommentsUi> = _comments.asStateFlow()

    private var currentPermalink: String? = null
    private var currentSorting: Sorting? = null

    fun loadPost(forceUpdate: Boolean) {
        val permalink = _permalink.value ?: run {
            _post.value = Resource.Error()
            _comments.value = CommentsUi(Resource.Error())
            return
        }
        if (permalink != currentPermalink || _sorting.value != currentSorting || forceUpdate) {
            currentPermalink = permalink
            currentSorting = _sorting.value
            loadPost(permalink, _sorting.value)
        }
    }

    private fun loadPost(permalink: String, sorting: Sorting) {
        viewModelScope.launch {
            repository.getPost(permalink, sorting)
                .onStart {
                    if (_post.value !is Resource.Success) _post.value = Resource.Loading()
                    _comments.value = CommentsUi(Resource.Loading())
                }
                .catch { e ->
                    val error: Resource.Error<Nothing> = when (e) {
                        is IOException -> Resource.Error(message = e.message)
                        is HttpException -> Resource.Error(e.code(), e.message())
                        else -> Resource.Error()
                    }
                    if (_post.value !is Resource.Success) _post.value = error
                    _comments.value = CommentsUi(error)
                }
                .collect { listings ->
                    val post = async { postMapper.dataToEntity(PostUtil.getPostData(listings)) }
                    val comments = async {
                        val list = commentMapper.dataToEntities(
                            PostUtil.getCommentsData(listings),
                            PostUtil.getPostData(listings)
                        )
                        flatten(list, DEPTH_LIMIT)
                    }
                    _post.value = Resource.Success(post.await())
                    _comments.value = CommentsUi(Resource.Success(comments.await()))
                }
        }
    }

    private suspend fun flatten(list: List<Comment>, depthLimit: Int): List<Comment> =
        withContext(defaultDispatcher) {
            buildList {
                for (comment in list) {
                    add(comment)
                    if (comment is CommentEntity && comment.depth < depthLimit) {
                        comment.isExpanded = true
                        addAll(flatten(comment.replies, depthLimit))
                    }
                }
            }
        }

    private fun visible(): MutableList<Comment>? =
        (_comments.value.resource as? Resource.Success)?.data?.toMutableList()

    private fun publish(list: List<Comment>) {
        _comments.value = CommentsUi(Resource.Success(list), _comments.value.revision + 1)
    }

    /** Collapses or expands a comment's replies. */
    fun toggleComment(comment: CommentEntity) {
        if (!comment.hasReplies) return
        viewModelScope.launch {
            val list = visible() ?: return@launch
            val index = list.indexOfFirst { it === comment }.takeIf { it >= 0 } ?: return@launch
            val start = index + 1
            if (comment.isExpanded) {
                comment.isExpanded = false
                var count = 0
                while (start + count < list.size && list[start + count].depth > comment.depth) count++
                comment.visibleReplyCount = count
                list.subList(start, start + count).clear()
            } else {
                comment.isExpanded = true
                list.addAll(start, expanded(comment.replies))
            }
            publish(list)
        }
    }

    private suspend fun expanded(comments: List<Comment>): List<Comment> =
        withContext(defaultDispatcher) {
            buildList {
                for (comment in comments) {
                    add(comment)
                    if (comment is CommentEntity && comment.isExpanded) addAll(expanded(comment.replies))
                }
            }
        }

    /** Loads the next batch of a "more comments" placeholder in place. */
    fun loadMore(more: MoreEntity, post: PostEntity) {
        if (more.isLoading) return
        viewModelScope.launch {
            val hasMore = more.more.size > LOAD_MORE_LIMIT
            val children = more.more.take(LOAD_MORE_LIMIT).joinToString(",")
            more.isLoading = true
            more.isError = false
            visible()?.let(::publish)
            try {
                val result: MoreChildren = repository.getMoreChildren(children, post.id).first()
                val loaded = withContext(defaultDispatcher) {
                    val entities = commentMapper.dataToEntities(result.json.data.things, null)
                        .filter { it.depth < COMMENT_DEPTH_LIMIT }
                    restoreHierarchy(entities, more.depth)
                }
                if (hasMore) {
                    more.more.subList(0, LOAD_MORE_LIMIT).clear()
                    more.count = (more.count - LOAD_MORE_LIMIT).coerceAtLeast(0)
                }
                val savedIds = savedCommentIds.first()
                loaded.forEach { comment ->
                    (comment as? CommentEntity)?.apply {
                        linkTitle = linkTitle ?: post.title
                        linkPermalink = linkPermalink ?: post.permalink
                        linkAuthor = linkAuthor ?: post.author
                        saved = savedIds.contains(name)
                    }
                }
                more.isLoading = false
                val list = visible() ?: return@launch
                val position = list.indexOfFirst { it === more }.takeIf { it >= 0 } ?: return@launch
                if (more.depth > 0) {
                    (list.find { it.name == more.parent } as? CommentEntity)?.replies?.apply {
                        remove(more)
                        addAll(loaded)
                        if (hasMore) add(more)
                    }
                }
                list.removeAt(position)
                list.addAll(position, expanded(loaded))
                if (hasMore) list.add(position + expanded(loaded).size, more)
                publish(list)
            } catch (e: Exception) {
                more.isLoading = false
                more.isError = true
                visible()?.let(::publish)
            }
        }
    }

    private fun restoreHierarchy(comments: List<Comment>, depth: Int): List<Comment> {
        val restored = mutableListOf<Comment>()
        for (i in comments.indices) {
            val comment = comments[i]
            if (comment.depth > depth) continue
            if (comment.depth < depth) break
            val next = comments.getOrNull(i + 1)
            if (comment is CommentEntity && next != null && next.depth > depth) {
                comment.replies.addAll(restoreHierarchy(comments.subList(i + 1, comments.size), depth + 1))
                comment.visibleReplyCount = comment.replies.size
                comment.isExpanded = true
            }
            restored.add(comment)
        }
        return restored
    }

    fun setSorting(sorting: Sorting) = _sorting.updateValue(sorting)
    fun setPermalink(permalink: String) = _permalink.updateValue(permalink)
    fun setSingleThread(singleThread: Boolean) = _singleThread.updateValue(singleThread)

    fun toggleSave(comment: CommentEntity) {
        toggleSaveComment(comment)
        comment.saved = !comment.saved
        visible()?.let(::publish)
    }

    companion object {
        private const val DEPTH_LIMIT = 3
        private const val LOAD_MORE_LIMIT = 100
        private const val COMMENT_DEPTH_LIMIT = 10
        private val DEFAULT_SORTING = Sorting(Sort.BEST)
    }
}
