package io.silv.data.tv


class UpdateShow(
    private val showRepository: ShowRepository
) {

    suspend fun await(showUpdate: TVShowUpdate): Boolean {
        return showRepository.updateShow(showUpdate)
    }
}